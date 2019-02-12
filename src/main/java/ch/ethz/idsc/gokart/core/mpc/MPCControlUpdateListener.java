// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import java.util.Objects;

import ch.ethz.idsc.retina.util.StartAndStoppable;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;

/* package */ abstract class MPCControlUpdateListener implements StartAndStoppable {
  /* package */ ControlAndPredictionSteps cns = null;
  // FIXME MH document that keeping istep outside the function is intended
  private int istep = 0;

  /** get the last step before a point int time
   * 
   * @param query time in Unit [s]
   * @return the control and prediction step before time */
  ControlAndPredictionStep getStep(Scalar time) {
    // ensure that old data is not used
    if (Objects.isNull(cns) || //
        Scalars.lessThan(MPCNative.OPEN_LOOP_TIME, time.subtract(cns.steps[0].gokartState.getTime())))
      return null;
    // FIXME MH without any assumptions on cns.steps[] this is not safe!
    // ... since istep may be greater than array !
    while (istep > 0 //
        && Scalars.lessThan( //
            time, //
            cns.steps[istep].gokartState.getTime())) {
      --istep;
    }
    while (istep + 1 < cns.steps.length //
        && Scalars.lessThan( //
            cns.steps[istep + 1].gokartState.getTime(), //
            time)) {
      ++istep;
    }
    // System.out.println("time: "+time.subtract(cns.steps[0].state.getTime())+"step: "+istep);
    return cns.steps[istep];
  }

  // TODO ideally this function should be final
  void getControlAndPredictionSteps(ControlAndPredictionSteps controlAndPredictionSteps) {
    this.cns = controlAndPredictionSteps;
  }

  /** the time that passed after the last step
   * 
   * @param query time in unit [s]
   * @return time passed since that last step in unit [s] */
  final Scalar getTimeSinceLastStep(Scalar time) {
    if (Objects.isNull(cns))
      return null;
    return time.subtract(getStep(time).gokartState.getTime());
  }
}
