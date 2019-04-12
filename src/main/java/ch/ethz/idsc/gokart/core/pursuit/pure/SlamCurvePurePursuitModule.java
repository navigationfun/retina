// code by mg
package ch.ethz.idsc.gokart.core.pursuit.pure;

import java.util.Optional;

import ch.ethz.idsc.demo.mg.slam.config.SlamDvsConfig;
import ch.ethz.idsc.gokart.core.pursuit.PursuitConfig;
import ch.ethz.idsc.gokart.core.pursuit.PursuitModule;
import ch.ethz.idsc.gokart.gui.top.ChassisGeometry;
import ch.ethz.idsc.owl.math.planar.PurePursuit;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;

/** pursuit controller for SLAM algorithm */
// XXX MG when only forward driving is supported, the speed should be Ramp'ed
public final class SlamCurvePurePursuitModule extends PursuitModule {
  private Optional<Tensor> optionalCurve = Optional.empty();

  public SlamCurvePurePursuitModule() {
    super(PursuitConfig.GLOBAL);
  }

  @Override // form AbstractModule
  protected void protected_first() {
    // ---
  }

  @Override // from AbstractModule
  protected void protected_last() {
    // ---
  }

  @Override // form PursuitModule
  protected Optional<Scalar> deriveHeading() {
    Optional<Scalar> ratio = getRatio();
    if (ratio.isPresent()) { // is look ahead beacon available?
      Scalar angle = ChassisGeometry.GLOBAL.steerAngleForTurningRatio(ratio.get());
      if (angleClip.isInside(angle)) // is look ahead beacon within steering range?
        return Optional.of(angle);
      System.err.println("beacon outside steering range");
    }
    return Optional.empty();
  }

  private Optional<Scalar> getRatio() {
    Optional<Tensor> optionalCurve = this.optionalCurve; // copy reference instead of synchronize
    if (optionalCurve.isPresent()) {
      if (optionalCurve.isPresent()) {
        PurePursuit purePursuit = PurePursuit.fromTrajectory( //
            optionalCurve.get(), SlamDvsConfig.eventCamera.slamPrcConfig.lookAheadMeter());
        return purePursuit.ratio();
      }
      return Optional.empty();
    }
    System.err.println("no curve in pure pursuit");
    return Optional.empty();
  }

  /** @param curve go kart frame coordinates */
  public void setCurve(Optional<Tensor> curve) {
    optionalCurve = curve;
  }
}