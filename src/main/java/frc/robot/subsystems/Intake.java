// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;
import frc.robot.Constants;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Intake extends SubsystemBase {

  /** Creates a new Intake. */
  private CANSparkMax intakeMotor = new CANSparkMax( Constants.INTAKEMOTOR, MotorType.kBrushless);
  public Intake() {

  }

  public void conePickup(double speed){
    intakeMotor.set(speed * -1);
  }

  public void conePut(double speed){
    intakeMotor.set(speed * 1);
  }

  public void cubePut(double speed){
    intakeMotor.set(speed * -1);
  }

  public void cubePickup(double speed){
    intakeMotor.set(speed * 1);
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }
}
