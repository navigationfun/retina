// code by mh
package ch.ethz.idsc.gokart.core.tvec;

import ch.ethz.idsc.gokart.calib.power.PowerLookupTable;
import ch.ethz.idsc.gokart.core.man.ManualConfig;
import ch.ethz.idsc.owl.car.math.AngularSlip;
import ch.ethz.idsc.retina.util.math.NonSI;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Chop;
import junit.framework.TestCase;

public class ImprovedNormalizedPredictiveTorqueVectoringTest extends TestCase {
  private static final PowerLookupTable POWER_LOOKUP_TABLE = PowerLookupTable.getInstance();

  public void testSimple() {
    TorqueVectoringConfig torqueVectoringConfig = new TorqueVectoringConfig();
    torqueVectoringConfig.staticCompensation = Quantity.of(0.4, SI.ACCELERATION.negate());
    torqueVectoringConfig.dynamicCorrection = Quantity.of(0, SI.SECOND);
    TorqueVectoringInterface torqueVectoringInterface = new ImprovedNormalizedPredictiveTorqueVectoring(torqueVectoringConfig);
    Scalar power = RealScalar.ZERO;
    Tensor powers = torqueVectoringInterface.powers( //
        new AngularSlip(Quantity.of(0, "m*s^-1"), Quantity.of(0, "m^-1"), Quantity.of(0, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    Chop._08.requireClose(Total.of(powers), power);
    assertEquals(powers, Tensors.vector(0, 0));
  }

  public void testZeroMean() {
    // this is only true when we have no torque vectoring
    TorqueVectoringConfig torqueVectoringConfig = new TorqueVectoringConfig();
    torqueVectoringConfig.staticCompensation = Quantity.of(0.4, SI.ACCELERATION.negate());
    torqueVectoringConfig.dynamicCorrection = Quantity.of(0, SI.SECOND);
    TorqueVectoringInterface torqueVectoringInterface = new ImprovedNormalizedPredictiveTorqueVectoring(torqueVectoringConfig);
    Scalar power = RealScalar.ZERO;
    Scalar velocity = Quantity.of(1, SI.VELOCITY);
    Tensor powers = torqueVectoringInterface.powers( //
        new AngularSlip(velocity, Quantity.of(1, "m^-1"), Quantity.of(0, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    // more complicated test
    Scalar maxcurr = ManualConfig.GLOBAL.torqueLimit;
    Scalar noPowerAcceleration = POWER_LOOKUP_TABLE.getAcceleration(Quantity.of(0, NonSI.ARMS), Quantity.of(1, "m*s^-1"));
    Scalar leftAcc = POWER_LOOKUP_TABLE.getAcceleration(powers.Get(0).multiply(maxcurr), velocity);
    Scalar rightAcc = POWER_LOOKUP_TABLE.getAcceleration(powers.Get(1).multiply(maxcurr), velocity);
    Chop._04.requireClose(Mean.of(Tensors.of(leftAcc, rightAcc)), noPowerAcceleration);
    // assertEquals(powers, Tensors.vector(-0.4, 0.4));
  }

  public void testAccelerationResponse() throws InterruptedException {
    // this is only true when we have no torque vectoring
    TorqueVectoringConfig torqueVectoringConfig = new TorqueVectoringConfig();
    torqueVectoringConfig.staticCompensation = Quantity.of(0, SI.ACCELERATION.negate());
    torqueVectoringConfig.dynamicCorrection = Quantity.of(0, SI.SECOND);
    torqueVectoringConfig.staticPrediction = Quantity.of(0.1, SI.ANGULAR_ACCELERATION.negate());
    TorqueVectoringInterface torqueVectoringInterface = new ImprovedNormalizedPredictiveTorqueVectoring(torqueVectoringConfig);
    Scalar power = RealScalar.ZERO;
    Scalar velocity = Quantity.of(1, SI.VELOCITY);
    Tensor powers0 = torqueVectoringInterface.powers( //
        new AngularSlip(velocity, Quantity.of(1, "m^-1"), Quantity.of(1, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    Thread.sleep(100);
    Tensor powers1 = torqueVectoringInterface.powers( //
        new AngularSlip(velocity, Quantity.of(1, "m^-1"), Quantity.of(1, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    Thread.sleep(200);
    Tensor powers2 = torqueVectoringInterface.powers( //
        new AngularSlip(velocity, Quantity.of(1.5, "m^-1"), Quantity.of(1, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    Thread.sleep(200);
    Tensor powers3 = torqueVectoringInterface.powers( //
        new AngularSlip(velocity, Quantity.of(1.5, "m^-1"), Quantity.of(1, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    Scalar power1l = PowerLookupTable.getInstance().getAcceleration(powers1.Get(0).multiply(ManualConfig.GLOBAL.torqueLimit), velocity);
    Scalar power1r = PowerLookupTable.getInstance().getAcceleration(powers1.Get(1).multiply(ManualConfig.GLOBAL.torqueLimit), velocity);
    Scalar power2l = PowerLookupTable.getInstance().getAcceleration(powers2.Get(0).multiply(ManualConfig.GLOBAL.torqueLimit), velocity);
    Scalar power2r = PowerLookupTable.getInstance().getAcceleration(powers2.Get(1).multiply(ManualConfig.GLOBAL.torqueLimit), velocity);
    Scalar power3l = PowerLookupTable.getInstance().getAcceleration(powers3.Get(0).multiply(ManualConfig.GLOBAL.torqueLimit), velocity);
    Scalar power3r = PowerLookupTable.getInstance().getAcceleration(powers3.Get(1).multiply(ManualConfig.GLOBAL.torqueLimit), velocity);
    System.out.println("1: " + power1l + "/" + power1r);
    System.out.println("1: " + power2l + "/" + power2r);
    System.out.println("1: " + power3l + "/" + power3r);
  }

  public void testSaturatedPositive() {
    TorqueVectoringConfig torqueVectoringConfig = new TorqueVectoringConfig();
    torqueVectoringConfig.staticCompensation = Quantity.of(0.4, SI.ACCELERATION.negate());
    torqueVectoringConfig.dynamicCorrection = Quantity.of(0, SI.SECOND);
    TorqueVectoringInterface torqueVectoringInterface = //
        new ImprovedNormalizedPredictiveTorqueVectoring(torqueVectoringConfig);
    Scalar power = RealScalar.ONE;
    Tensor powers = torqueVectoringInterface.powers( //
        new AngularSlip(Quantity.of(-2, "m*s^-1"), Quantity.of(1, "m^-1"), Quantity.of(3, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    // it's 0.9999999...
    Scalar between = Norm._2.between(powers, Tensors.vector(1, 1));
    assertTrue(Scalars.lessThan(between, RealScalar.of(0.02)));
    // assertTrue(Chop._04.close(powers, Tensors.vector(1, 1)));
  }

  public void testSaturatedNegative() {
    TorqueVectoringConfig torqueVectoringConfig = new TorqueVectoringConfig();
    torqueVectoringConfig.staticCompensation = Quantity.of(0.4, SI.ACCELERATION.negate());
    torqueVectoringConfig.dynamicCorrection = Quantity.of(0, SI.SECOND);
    TorqueVectoringInterface torqueVectoringInterface = //
        new ImprovedNormalizedPredictiveTorqueVectoring(torqueVectoringConfig);
    Scalar power = RealScalar.ONE.negate();
    Tensor powers = torqueVectoringInterface.powers( //
        new AngularSlip(Quantity.of(-2, "m*s^-1"), Quantity.of(1, "m^-1"), Quantity.of(3, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    Scalar between = Norm._2.between(powers, Tensors.vector(-1, -1));
    assertTrue(Scalars.lessThan(between, RealScalar.of(0.02)));
  }

  /* Scalar expectedRotationPerMeterDriven
   * Scalar meanTangentSpeed
   * Scalar angularSlip
   * Scalar power
   * Scalar realRotation */
  public void testTorqueWhenUndersteering() {
    TorqueVectoringConfig tvc = new TorqueVectoringConfig();
    tvc.staticCompensation = Quantity.of(0.4, SI.ACCELERATION.negate());
    tvc.dynamicCorrection = Quantity.of(0.2, SI.SECOND);
    TorqueVectoringInterface torqueVectoringInterface = new ImprovedNormalizedPredictiveTorqueVectoring(tvc);
    Scalar power = RealScalar.ZERO;
    // brutal oversteering -> reaction should be that there is no differential torque
    Tensor powers = torqueVectoringInterface.powers( //
        new AngularSlip(Quantity.of(3, "m*s^-1"), Quantity.of(1, "m^-1"), Quantity.of(1, "s^-1"), Quantity.of(0, "s^-1")), //
        power);
    assertTrue(Scalars.lessThan(powers.Get(0), powers.Get(1)));
  }

  public void testNoTorqueWhenOversteering() {
    TorqueVectoringConfig tvc = new TorqueVectoringConfig();
    tvc.staticCompensation = Quantity.of(0.4, SI.ACCELERATION.negate());
    tvc.dynamicCorrection = Quantity.of(0.2, SI.SECOND);
    TorqueVectoringInterface torqueVectoringInterface = new ImprovedNormalizedPredictiveTorqueVectoring(tvc);
    Scalar power = RealScalar.ZERO;
    // brutal oversteering -> reaction should be that there is no differential torque
    Tensor powers = torqueVectoringInterface.powers( //
        new AngularSlip(Quantity.of(3, "m*s^-1"), Quantity.of(-1, "m^-1"), Quantity.of(1, "s^-1"), Quantity.of(3, "s^-1")), //
        power);
    assertEquals(powers.Get(0), powers.Get(1));
  }

  public void testFilter() {
    TorqueVectoringConfig torqueVectoringConfig = new TorqueVectoringConfig();
    torqueVectoringConfig.rollingAverageRatio = RealScalar.of(0.5);
    ImprovedNormalizedPredictiveTorqueVectoring inpdv = //
        new ImprovedNormalizedPredictiveTorqueVectoring(torqueVectoringConfig);
    Scalar acc0 = inpdv.estimateRotationAcceleration(Quantity.of(1, "s^-1"), 0.1);
    Chop._13.requireClose(acc0, Quantity.of(0, SI.ANGULAR_ACCELERATION));
    Scalar acc1 = inpdv.estimateRotationAcceleration(Quantity.of(1.2, "s^-1"), 0.1);
    Chop._13.requireClose(acc1, Quantity.of(1, SI.ANGULAR_ACCELERATION));
    Scalar acc2 = inpdv.estimateRotationAcceleration(Quantity.of(1.3, "s^-1"), 0.1);
    Chop._13.requireClose(acc2, Quantity.of(1, SI.ANGULAR_ACCELERATION));
    Scalar acc3 = inpdv.estimateRotationAcceleration(Quantity.of(1.5, "s^-1"), 0.1);
    Chop._13.requireClose(acc3, Quantity.of(1.5, SI.ANGULAR_ACCELERATION));
    Scalar acc4 = inpdv.estimateRotationAcceleration(Quantity.of(1.2, "s^-1"), 0.2);
    Chop._13.requireClose(acc4, Quantity.of(0, SI.ANGULAR_ACCELERATION));
    Scalar acc5 = inpdv.estimateRotationAcceleration(Quantity.of(1.3, "s^-1"), 0.1);
    Chop._13.requireClose(acc5, Quantity.of(0.5, SI.ANGULAR_ACCELERATION));
  }
}
