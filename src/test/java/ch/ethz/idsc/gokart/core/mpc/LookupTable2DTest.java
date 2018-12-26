// code by mh
package ch.ethz.idsc.gokart.core.mpc;

import java.io.File;
import java.util.Random;
import java.util.function.BinaryOperator;

import ch.ethz.idsc.retina.util.math.NonSI;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Clip;
import junit.framework.TestCase;

public class LookupTable2DTest extends TestCase {
  public void testConsistency() throws Exception {
    // units not part of this unit test
    // save to file and reload again
    Random random = new Random();
    final int n = 30;
    float table[][] = new float[n][n];
    for (int i0 = 0; i0 < n; ++i0)
      for (int i1 = 0; i1 < n; ++i1)
        table[i0][i1] = random.nextFloat();
    LookupTable2D lookupTable = new LookupTable2D( //
        Tensors.matrixFloat(table), //
        Clip.absoluteOne(), //
        Clip.absoluteOne(), //
        SI.METER);
    final File file = new File("testLookupTable.object");
    Export.object(file, lookupTable);
    LookupTable2D lookupTable2 = Import.object(file);
    file.delete();
    assertEquals(Tensors.matrixFloat(table), lookupTable2.tensor);
  }

  public void testFidelity() throws Exception {
    BinaryOperator<Scalar> function = (u, v) -> u;
    final int DimN = 1000;
    final Scalar fidelityLimit = RealScalar.of(0.001);
    final int testN = 100;
    LookupTable2D lookupTable2D = LookupTable2D.build(//
        function, //
        DimN, //
        DimN, //
        Clip.function(-0.3, 1.2), //
        Clip.function(-0.7, 3.1), //
        SI.ONE);
    Random rand = new Random();
    for (int count = 0; count < testN; ++count) {
      Scalar x = RealScalar.of(rand.nextFloat());
      Scalar y = RealScalar.of(rand.nextFloat());
      Scalar out = lookupTable2D.lookup(x, y);
      Scalar refOut = function.apply(x, y);
      Scalar diff = out.subtract(refOut).abs();
      // System.out.println("For X="+ x + " and Y="+y+": "+diff);
      // System.out.println("out="+out+ " /ref="+refOut);
      assertTrue(Scalars.lessThan(diff, fidelityLimit));
    }
  }

  public void testInversion() throws Exception {
    final int DimN = 100;
    final Scalar xMin = RealScalar.of(-0.3);
    final Scalar xMax = RealScalar.of(+1.2);
    final Scalar inversionLimit = RealScalar.of(0.001);
    final int testN = 100;
    LookupTable2D lookupTable2D = LookupTable2D.build(//
        Scalar::add, //
        DimN, //
        DimN, //
        Clip.function(-0.3, 1.2), //
        Clip.function(-0.7, 3.1), //
        SI.ONE);
    LookupTable2D inverseLookupTable = lookupTable2D.getInverseLookupTableBinarySearch(//
        Scalar::add, //
        0, DimN, //
        DimN, //
        Clip.function(-5, 5));
    Random rand = new Random();
    for (int count = 0; count < testN; ++count) {
      Scalar x = RealScalar.of(rand.nextFloat());
      Scalar y = RealScalar.of(rand.nextFloat());
      Scalar out = lookupTable2D.lookup(x, y);
      Scalar xb = inverseLookupTable.lookup(out, y);
      Scalar diff = x.subtract(xb).abs();
      // System.out.println("For X="+ x + " and Y="+y+": "+diff);
      // System.out.println("x="+x+ " /xb="+xb);
      assertTrue(Scalars.lessThan(diff, inversionLimit));
    }
    // check if values outside limits of the original lookup table are enforced:
    Scalar xb = inverseLookupTable.lookup(RealScalar.of(-5), RealScalar.of(0));
    assertTrue(Scalars.lessThan(xb.subtract(xMin).abs(), inversionLimit));
    xb = inverseLookupTable.lookup(RealScalar.of(5), RealScalar.of(0));
    assertTrue(Scalars.lessThan(xb.subtract(xMax).abs(), inversionLimit));
  }

  public void testInversion2() throws Exception {
    final int DimN = 100;
    final Scalar inversionLimit = RealScalar.of(0.001);
    final Scalar yMin = RealScalar.of(-0.7);
    final Scalar yMax = RealScalar.of(3.1);
    final int testN = 100;
    LookupTable2D lookUpTable2D = LookupTable2D.build(//
        Scalar::add, //
        DimN, //
        DimN, //
        Clip.function(-0.3, 1.2), //
        Clip.function(-0.7, 3.1), //
        SI.ONE);
    LookupTable2D inverseLookupTable = lookUpTable2D.getInverseLookupTableBinarySearch(//
        Scalar::add, //
        1, //
        DimN, //
        DimN, //
        Clip.function(-5, 5));
    Random rand = new Random();
    for (int count = 0; count < testN; ++count) {
      Scalar x = RealScalar.of(rand.nextFloat());
      Scalar y = RealScalar.of(rand.nextFloat());
      Scalar out = lookUpTable2D.lookup(x, y);
      Scalar yb = inverseLookupTable.lookup(x, out);
      Scalar diff = y.subtract(yb).abs();
      // System.out.println("For X="+ x + " and Y="+y+": "+diff);
      // System.out.println("y="+y+ " /yb="+yb);
      assertTrue(Scalars.lessThan(diff, inversionLimit));
    }
    // check if values outside limits of the original lookup table are enforced:
    Scalar yb = inverseLookupTable.lookup(RealScalar.of(0), RealScalar.of(-4));
    assertTrue(Scalars.lessThan(yb.subtract(yMin).abs(), inversionLimit));
    yb = inverseLookupTable.lookup(RealScalar.of(0), RealScalar.of(+4));
    assertTrue(Scalars.lessThan(yb.subtract(yMax).abs(), inversionLimit));
  }

  public void testWithPowerFunction() {
    final int DimN = 250;
    // higher limit because of scaling of output [-2300, 2300]
    final Scalar inversionLimit = Quantity.of(2, NonSI.ARMS);
    Clip clip = Clip.function(Quantity.of(-2300, NonSI.ARMS), Quantity.of(+2300, NonSI.ARMS));
    final Scalar yMin = Quantity.of(-10, SI.VELOCITY);
    final Scalar yMax = Quantity.of(+10, SI.VELOCITY);
    final int testN = 100;
    LookupTable2D lookupTable2D = LookupTable2D.build(//
        MotorFunction::getAccelerationEstimation, //
        DimN, //
        DimN, //
        clip, //
        Clip.function(yMin, yMax), //
        SI.ACCELERATION);
    LookupTable2D inverseLookupTable = lookupTable2D.getInverseLookupTableBinarySearch(//
        MotorFunction::getAccelerationEstimation, //
        0, //
        DimN, //
        DimN, //
        Clip.function( //
            Quantity.of(-2, SI.ACCELERATION), //
            Quantity.of(+2, SI.ACCELERATION)));
    Random rand = new Random();
    for (int count = 0; count < testN; ++count) {
      Scalar x = Quantity.of(rand.nextFloat() * 1000, NonSI.ARMS);
      Scalar y = Quantity.of(rand.nextFloat(), SI.VELOCITY);
      Scalar out = lookupTable2D.lookup(x, y);
      Scalar xb = inverseLookupTable.lookup(out, y);
      Scalar diff = x.subtract(xb).abs();
      if (Scalars.lessThan(inversionLimit, diff)) {
        System.out.println("For X=" + x + " and Y=" + y + ": " + diff);
        System.out.println("out: " + out);
        System.out.println("fun out: " + MotorFunction.getAccelerationEstimation(x, y));
        System.out.println("x=" + x + " /xb=" + xb);
      }
    }
  }
}