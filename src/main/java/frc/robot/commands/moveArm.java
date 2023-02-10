// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.CommandBase;
import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.Constants;
import frc.robot.subsystems.ArmSubsystem;

public class MoveArm extends CommandBase {
  /** Creates a new moveArm. */
 private ArmSubsystem armSpeed;
 private DoubleSupplier armValue;

  public MoveArm(ArmSubsystem armSpeed,DoubleSupplier armValue ) {
    // Use addRequirements() here to declare subsystem dependencies.
    this.armSpeed = armSpeed; 
    this.armValue = armValue;
    addRequirements(this.armSpeed);

  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    armSpeed.unlock();
  }
    
  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    double armPower = armValue.getAsDouble()*Constants.ARM_POWER_SCALING;
    armSpeed.moveArm(armPower);
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    armSpeed.moveArm(0);
    armSpeed.lock();
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }
}
