// code by mh
package ch.ethz.idsc.gokart.core.sound;

import ch.ethz.idsc.gokart.core.sound.GokartSoundCreator.Exciter;
import ch.ethz.idsc.gokart.core.sound.GokartSoundCreator.MotorState;

public class SimpleExciter extends Exciter {
  private final float absFrequency;
  private final float relFrequency;
  private final float powerFactor;
  private float sinePosition = 0;
  private float dsinePosition;

  public SimpleExciter(float absFrequency, float relFrequency, float powerFactor) {
    this.absFrequency = absFrequency;
    this.relFrequency = relFrequency;
    this.powerFactor = powerFactor;
  }

  @Override
  public float getNextValue(MotorState state, float dt) {
    dsinePosition = dt * (state.speed * relFrequency + absFrequency);
    sinePosition += dsinePosition;
    if (sinePosition > Math.PI * 2)
      sinePosition -= Math.PI * 2;
    float sineVal = (float) Math.sin(sinePosition);
    float powered = (float) (Math.signum(sineVal) * Math.pow(Math.abs(sineVal), powerFactor * state.power + 1));
    return powered;
  }
}
