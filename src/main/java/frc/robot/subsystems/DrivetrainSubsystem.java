package frc.robot.subsystems;

import static frc.robot.Constants.BACK_LEFT_MODULE_DRIVE_MOTOR;
import static frc.robot.Constants.BACK_LEFT_MODULE_STEER_ENCODER;
import static frc.robot.Constants.BACK_LEFT_MODULE_STEER_MOTOR;
import static frc.robot.Constants.BACK_LEFT_MODULE_STEER_OFFSET;
import static frc.robot.Constants.BACK_RIGHT_MODULE_DRIVE_MOTOR;
import static frc.robot.Constants.BACK_RIGHT_MODULE_STEER_ENCODER;
import static frc.robot.Constants.BACK_RIGHT_MODULE_STEER_MOTOR;
import static frc.robot.Constants.BACK_RIGHT_MODULE_STEER_OFFSET;
import static frc.robot.Constants.DERIVATIVE_COEFFICENT;
import static frc.robot.Constants.DRIVETRAIN_TRACKWIDTH_METERS;
import static frc.robot.Constants.DRIVETRAIN_WHEELBASE_METERS;
import static frc.robot.Constants.FRONT_LEFT_MODULE_DRIVE_MOTOR;
import static frc.robot.Constants.FRONT_LEFT_MODULE_STEER_ENCODER;
import static frc.robot.Constants.FRONT_LEFT_MODULE_STEER_MOTOR;
import static frc.robot.Constants.FRONT_LEFT_MODULE_STEER_OFFSET;
import static frc.robot.Constants.FRONT_RIGHT_MODULE_DRIVE_MOTOR;
import static frc.robot.Constants.FRONT_RIGHT_MODULE_STEER_ENCODER;
import static frc.robot.Constants.FRONT_RIGHT_MODULE_STEER_MOTOR;
import static frc.robot.Constants.FRONT_RIGHT_MODULE_STEER_OFFSET;
import static frc.robot.Constants.INTEGRAL_COEFFICENT;
import static frc.robot.Constants.PROPORTIONAL_COEFFICENT;

import com.kauailabs.navx.frc.AHRS;
import com.swervedrivespecialties.swervelib.Mk4SwerveModuleHelper;
import com.swervedrivespecialties.swervelib.SdsModuleConfigurations;
import com.swervedrivespecialties.swervelib.SwerveModule;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.DoubleTopic;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Robot;

public class DrivetrainSubsystem extends SubsystemBase {
        public static final double MAX_VOLTAGE = 12;
        public static final double MAX_VELOCITY_METERS_PER_SECOND = 6380.0 / 60.0 *
                        SdsModuleConfigurations.MK4_L3.getDriveReduction() *
                        SdsModuleConfigurations.MK4_L3.getWheelDiameter() * Math.PI;
        public static final double MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND = MAX_VELOCITY_METERS_PER_SECOND /
                        Math.hypot(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0);

        private final SwerveDriveKinematics kinematics = new SwerveDriveKinematics(
                        // Front left
                        new Translation2d(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Front right
                        new Translation2d(DRIVETRAIN_TRACKWIDTH_METERS / 2.0, -DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Back left
                        new Translation2d(-DRIVETRAIN_TRACKWIDTH_METERS / 2.0, DRIVETRAIN_WHEELBASE_METERS / 2.0),
                        // Back right
                        new Translation2d(-DRIVETRAIN_TRACKWIDTH_METERS / 2.0, -DRIVETRAIN_WHEELBASE_METERS / 2.0));

        private final AHRS navx = new AHRS(SPI.Port.kMXP, (byte) 200);

        private final SwerveModule frontLeftModule;
        private final SwerveModule frontRightModule;
        private final SwerveModule backLeftModule;
        private final SwerveModule backRightModule;

        private double[] positionMeters = new double[4];
        private double lastUpdateTime = 0;

        private SwerveModuleState[] states;
        private SwerveDriveOdometry odometry;
        private Pose2d robotPose;
        private Field2d field = new Field2d();
        private double simRotation = 0;
        private final Pose2d RED_ORIGIN = new Pose2d(new Translation2d(Constants.RED_ORIGIN_POS_X_METERS, Constants.RED_ORIGIN_POS_Y_METERS),Rotation2d.fromDegrees(Constants.RED_ORIGIN_ROTATION_DEG));

        private NetworkTable table = NetworkTableInstance.getDefault().getTable("Drivetrain");
        private DoubleTopic gyroTopic = table.getDoubleTopic("Gyro Rotation");
        private DoublePublisher gyroReading = gyroTopic.publish();

        private ChassisSpeeds chassisSpeeds = new ChassisSpeeds(0.0, 0.0, 0.0);

        private Rotation2d rotationTarget;
        private Translation2d translationTarget;

        private PIDController yPID = new PIDController(PROPORTIONAL_COEFFICENT, INTEGRAL_COEFFICENT,
                        DERIVATIVE_COEFFICENT);
        private PIDController xPID = new PIDController(PROPORTIONAL_COEFFICENT, INTEGRAL_COEFFICENT,
                        DERIVATIVE_COEFFICENT);
        private PIDController rotationPID = new PIDController(PROPORTIONAL_COEFFICENT, INTEGRAL_COEFFICENT,
                        DERIVATIVE_COEFFICENT);

        public DrivetrainSubsystem() {
                states = kinematics.toSwerveModuleStates(chassisSpeeds);
                SmartDashboard.putData("Field Sim", field);

                ShuffleboardTab tab = Shuffleboard.getTab("Drivetrain");

                frontLeftModule = Mk4SwerveModuleHelper.createNeo(
                                tab.getLayout("Front Left Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(0, 0),
                                Mk4SwerveModuleHelper.GearRatio.L3,
                                FRONT_LEFT_MODULE_DRIVE_MOTOR,
                                FRONT_LEFT_MODULE_STEER_MOTOR,
                                FRONT_LEFT_MODULE_STEER_ENCODER,
                                FRONT_LEFT_MODULE_STEER_OFFSET);

                frontRightModule = Mk4SwerveModuleHelper.createNeo(
                                tab.getLayout("Front Right Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(2, 0),
                                Mk4SwerveModuleHelper.GearRatio.L3,
                                FRONT_RIGHT_MODULE_DRIVE_MOTOR,
                                FRONT_RIGHT_MODULE_STEER_MOTOR,
                                FRONT_RIGHT_MODULE_STEER_ENCODER,
                                FRONT_RIGHT_MODULE_STEER_OFFSET);

                backLeftModule = Mk4SwerveModuleHelper.createNeo(
                                tab.getLayout("Back Left Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(4, 0),
                                Mk4SwerveModuleHelper.GearRatio.L3,
                                BACK_LEFT_MODULE_DRIVE_MOTOR,
                                BACK_LEFT_MODULE_STEER_MOTOR,
                                BACK_LEFT_MODULE_STEER_ENCODER,
                                BACK_LEFT_MODULE_STEER_OFFSET);

                backRightModule = Mk4SwerveModuleHelper.createNeo(
                                tab.getLayout("Back Right Module", BuiltInLayouts.kList)
                                                .withSize(2, 4)
                                                .withPosition(6, 0),
                                Mk4SwerveModuleHelper.GearRatio.L3,
                                BACK_RIGHT_MODULE_DRIVE_MOTOR,
                                BACK_RIGHT_MODULE_STEER_MOTOR,
                                BACK_RIGHT_MODULE_STEER_ENCODER,
                                BACK_RIGHT_MODULE_STEER_OFFSET);
                
                odometry = new SwerveDriveOdometry(
                        kinematics,
                        getGyroscopeRotation(),
                        getModulePositions()
                );
        }

        public void zeroGyroscope() {
                navx.zeroYaw();
        }

        private SwerveModulePosition[] getModulePositions() {
                return new SwerveModulePosition[] {
                        new SwerveModulePosition(positionMeters[0], states[0].angle),
                        new SwerveModulePosition(positionMeters[1], states[1].angle),
                        new SwerveModulePosition(positionMeters[2], states[2].angle),
                        new SwerveModulePosition(positionMeters[3], states[3].angle)
                };
        }

        public void resetPose(Pose2d pose) {
                odometry.resetPosition(
                        getGyroscopeRotation(),
                        getModulePositions(), 
                        pose
                );
        }

        private void updatePose() {
                if(Robot.isSimulation()) {
                        positionMeters[0] += states[0].speedMetersPerSecond * (Timer.getFPGATimestamp() - lastUpdateTime);
                        positionMeters[1] += states[1].speedMetersPerSecond * (Timer.getFPGATimestamp() - lastUpdateTime);
                        positionMeters[2] += states[2].speedMetersPerSecond * (Timer.getFPGATimestamp() - lastUpdateTime);
                        positionMeters[3] += states[3].speedMetersPerSecond * (Timer.getFPGATimestamp() - lastUpdateTime);
                        simRotation += kinematics.toChassisSpeeds(states).omegaRadiansPerSecond * (Timer.getFPGATimestamp() - lastUpdateTime);
                }else {
                        positionMeters[0] += frontLeftModule.getDriveVelocity() * (Timer.getFPGATimestamp() - lastUpdateTime); // (m / s) * delta t = m
                        positionMeters[1] += frontRightModule.getDriveVelocity() * (Timer.getFPGATimestamp() - lastUpdateTime);
                        positionMeters[2] += backLeftModule.getDriveVelocity() * (Timer.getFPGATimestamp() - lastUpdateTime);
                        positionMeters[3] += backRightModule.getDriveVelocity() * (Timer.getFPGATimestamp() - lastUpdateTime);
                }
                lastUpdateTime = Timer.getFPGATimestamp();

                robotPose = odometry.update(
                        getGyroscopeRotation(),
                        getModulePositions()
                );

                if(DriverStation.getAlliance() == Alliance.Red && robotPose != null){
                        field.setRobotPose(RED_ORIGIN.transformBy(new Transform2d(robotPose.getTranslation(), robotPose.getRotation())));
                } else {
                        field.setRobotPose(getPose());
                }
        }

        public SwerveDriveKinematics getKinematics() {
                return kinematics;
        }

        public Pose2d getPose() {
                return robotPose;
        }

        public Rotation2d getRotation() {
                return robotPose.getRotation();
        }

        public void setSwerveStates(SwerveModuleState[] states) {
                this.chassisSpeeds = kinematics.toChassisSpeeds(states);
        }

        private Rotation2d getGyroscopeRotation() {

                if(Robot.isSimulation()) {
                        return Rotation2d.fromRadians(simRotation);
                }

                if (navx.isMagnetometerCalibrated()) {
                        return Rotation2d.fromDegrees(navx.getFusedHeading());
                }

                return Rotation2d.fromDegrees(360.0 - navx.getYaw());
        }

        public void drive(ChassisSpeeds chassisSpeeds) {
                this.chassisSpeeds = chassisSpeeds;
        }

        /* Convience method, calls drive. Uses field relative controls. */
        public void drive(double x, double y, double rotation) {
                drive(ChassisSpeeds.fromFieldRelativeSpeeds(x, y, rotation, getGyroscopeRotation()));
        }

        public void setRotationTarget(Rotation2d rotationTarget) {
                this.rotationTarget = rotationTarget;
        }

        public Rotation2d getRotationTarget() {
                return rotationTarget;
        }

        public void setTranslationTarget(Translation2d translationTarget) {
                this.translationTarget = translationTarget;
        }

        public Translation2d getTranslationTarget() {
                return translationTarget;
        }

        @Override
        public void periodic() {
                if(rotationTarget != null && chassisSpeeds.omegaRadiansPerSecond == 0) {
                        chassisSpeeds.omegaRadiansPerSecond = rotationPID.calculate(getRotation().minus(rotationTarget).getRadians());
                }

                if(translationTarget != null && chassisSpeeds.vxMetersPerSecond == 0 && chassisSpeeds.vyMetersPerSecond == 0) {
                        chassisSpeeds.vxMetersPerSecond = xPID.calculate(getPose().getTranslation().minus(translationTarget).getX());
                        chassisSpeeds.vyMetersPerSecond = yPID.calculate(getPose().getTranslation().minus(translationTarget).getY());
                }

                states = kinematics.toSwerveModuleStates(chassisSpeeds);
                SwerveDriveKinematics.desaturateWheelSpeeds(states, MAX_VELOCITY_METERS_PER_SECOND);

                frontLeftModule.set(states[0].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE, states[0].angle.getRadians());
                frontRightModule.set(states[1].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE, states[1].angle.getRadians());
                backLeftModule.set(states[2].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE, states[2].angle.getRadians());
                backRightModule.set(states[3].speedMetersPerSecond / MAX_VELOCITY_METERS_PER_SECOND * MAX_VOLTAGE, states[3].angle.getRadians());

                updatePose();

                gyroReading.set(getGyroscopeRotation().getDegrees());
        }
}
