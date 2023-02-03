// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import frc.robot.commands.DefaultDriveCommand;
import frc.robot.subsystems.DrivetrainSubsystem;
import frc.robot.subsystems.Vision;
import frc.robot.util.JoystickModification;
import frc.robot.commands.IntakeCone;
import frc.robot.commands.IntakeCube;
import frc.robot.commands.DropCube;
import frc.robot.commands.DropCone;
import frc.robot.subsystems.Intake;
public class RobotContainer {
  private final Vision vision = new Vision();
  private final DrivetrainSubsystem drivetrainSubsystem = new DrivetrainSubsystem();
  private final JoystickModification mod = new JoystickModification();
  private final Joystick driverController = new Joystick(Constants.DRIVER_CONTROLLER);
  private final JoystickButton driverControllerAButton = new JoystickButton(driverController, Constants.A_BUTTON);
  private final JoystickButton driverControllerBButton = new JoystickButton(driverController, Constants.B_BUTTON);
  private final JoystickButton driverControllerXButton = new JoystickButton(driverController, Constants.X_BUTTON);
  private final JoystickButton driverControllerYButton = new JoystickButton(driverController, Constants.Y_BUTTON);
  private final Intake intake = new Intake();

  private final JoystickButton backButton = new JoystickButton(driverController, Constants.BACK_BUTTON);
  private final JoystickButton bButton = new JoystickButton(driverController, Constants.B_BUTTON);

  public RobotContainer() {
    configureBindings();
  }

  private void configureBindings() {
    drivetrainSubsystem.setDefaultCommand(new DefaultDriveCommand(
        drivetrainSubsystem,
        () -> -mod.modifyAxis(driverController.getRawAxis(Constants.LEFT_Y_AXIS))
            * DrivetrainSubsystem.MAX_VELOCITY_METERS_PER_SECOND,
        () -> -mod.modifyAxis(driverController.getRawAxis(Constants.LEFT_X_AXIS))
            * DrivetrainSubsystem.MAX_VELOCITY_METERS_PER_SECOND,
        () -> -mod.modifyAxis(driverController.getRawAxis(Constants.RIGHT_X_AXIS))
            * DrivetrainSubsystem.MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND));

    backButton.onTrue(new InstantCommand(drivetrainSubsystem::zeroGyroscope));

    bButton.toggleOnTrue(new DefaultDriveCommand(
        drivetrainSubsystem,
        () -> -mod.modifyAxis(driverController.getRawAxis(Constants.LEFT_Y_AXIS))
            * DrivetrainSubsystem.MAX_VELOCITY_METERS_PER_SECOND,
        () -> -mod.modifyAxis(driverController.getRawAxis(Constants.LEFT_X_AXIS))
            * DrivetrainSubsystem.MAX_VELOCITY_METERS_PER_SECOND,
        () -> -mod.modifyAxis(driverController.getRawAxis(Constants.RIGHT_X_AXIS))
            * DrivetrainSubsystem.MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND,
        new Rotation2d(Constants.DRIVE_ROTATION_TARGET_DEGREES)));

        driverControllerAButton.onTrue(new DropCone(intake));
        driverControllerBButton.onTrue(new DropCube(intake));
        driverControllerXButton.onTrue(new IntakeCone(intake));
  }

  public Command getAutonomousCommand() {
    return null;
  }
}
