// code by jph
package ch.ethz.idsc.retina.gui.gokart;

import java.util.Optional;

import ch.ethz.idsc.retina.dev.misc.MiscGetEvent;
import ch.ethz.idsc.retina.dev.misc.MiscGetListener;
import ch.ethz.idsc.retina.dev.misc.MiscSocket;
import ch.ethz.idsc.retina.dev.rimo.RimoPutEvent;
import ch.ethz.idsc.retina.dev.rimo.RimoPutProvider;
import ch.ethz.idsc.retina.dev.rimo.RimoSocket;
import ch.ethz.idsc.retina.dev.zhkart.ProviderRank;
import ch.ethz.idsc.retina.sys.AbstractModule;
import ch.ethz.idsc.retina.util.data.TimedFuse;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Clip;

/** sends stop command if steer battery voltage is outside of valid range
 * or if emergency flag is set in {@link MiscGetEvent} */
public class MiscEmergencyModule extends AbstractModule implements MiscGetListener, RimoPutProvider {
  private static final Clip VOLTAGE_RANGE = Clip.function( //
      Quantity.of(10.7, "V"), //
      Quantity.of(13.0, "V"));
  // ---
  private TimedFuse timedFuse = new TimedFuse(0.5);
  private boolean flag = false;

  @Override
  protected void first() throws Exception {
    MiscSocket.INSTANCE.addGetListener(this);
    RimoSocket.INSTANCE.addPutProvider(this);
  }

  @Override
  protected void last() {
    RimoSocket.INSTANCE.removePutProvider(this);
    MiscSocket.INSTANCE.removeGetListener(this);
  }

  @Override
  public ProviderRank getProviderRank() {
    return ProviderRank.EMERGENCY;
  }

  @Override
  public Optional<RimoPutEvent> putEvent() {
    return Optional.ofNullable(timedFuse.isBlown() || flag ? RimoPutEvent.STOP : null);
  }

  @Override
  public void getEvent(MiscGetEvent miscGetEvent) {
    timedFuse.register(VOLTAGE_RANGE.isOutside(miscGetEvent.getSteerBatteryVoltage()));
    flag |= miscGetEvent.isEmergency();
  }
}