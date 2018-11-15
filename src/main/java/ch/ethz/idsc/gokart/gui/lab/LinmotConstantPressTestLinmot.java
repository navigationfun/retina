// code by jph
package ch.ethz.idsc.gokart.gui.lab;

import java.util.Optional;

import ch.ethz.idsc.owl.math.state.ProviderRank;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutEvent;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutOperation;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutProvider;
import ch.ethz.idsc.tensor.Scalar;

/* package */ class LinmotConstantPressTestLinmot implements LinmotPutProvider {
  private boolean isActive = false;
  private short restingPosition;
  private Boolean turnOff = false;

  @Override
  public ProviderRank getProviderRank() {
    return ProviderRank.TESTING;
  }

  @Override
  public Optional<LinmotPutEvent> putEvent() {
    if (turnOff) {
      return isActive//
          ? Optional.of(LinmotPutOperation.INSTANCE.turnOff())
          : Optional.empty();
    } else {
      return isActive //
          ? Optional.of(LinmotPutOperation.INSTANCE.absolutePosition(restingPosition))
          : Optional.empty();
    }
  }
  
  public boolean getIsActive() {
    return isActive;
  }
  
  public boolean getIsOff() {
    return turnOff;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public void setOff(boolean off) {
    turnOff = off;
  }
  
  public void setPosition(short pos) {
    restingPosition = pos;
  }
}
