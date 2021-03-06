// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import ch.ethz.idsc.retina.util.StartAndStoppable;
import ch.ethz.idsc.tensor.Scalar;

// TODO MH is it necessary to MPCStateProviderClient
/* package */ abstract class MPCBraking extends MPCControlUpdateCapture implements //
    MPCStateProviderClient, StartAndStoppable {
  /** @param time
   * @return relative brake position in the interval [0, 1] */
  abstract Scalar getBraking(Scalar time);
}
