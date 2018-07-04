// code by mg
package ch.ethz.idsc.demo.mg.util;

import java.util.Arrays;
import java.util.Comparator;

import ch.ethz.idsc.demo.mg.slam.MapProvider;
import ch.ethz.idsc.demo.mg.slam.SlamParticle;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;

// collection of public static void methods to handle SlamParticle
// TODO change to interface with static implementations?
public class SlamParticleUtil {
  public static final Comparator<SlamParticle> SlamCompare = new Comparator<SlamParticle>() {
    @Override
    public int compare(SlamParticle o1, SlamParticle o2) {
      if (o1.getParticleLikelihood() < o2.getParticleLikelihood())
        return 1;
      if (o1.getParticleLikelihood() > o2.getParticleLikelihood())
        return -1;
      return 0;
    }
  };

  /** initial distribution with a given initial pose
   * 
   * @param pose
   * @param slamParticles */
  public static void setInitialDistribution(SlamParticle[] slamParticles, Tensor pose, double linVelAvg, double linVelStd, double angVelStd) {
    double initLikelihood = (double) 1 / slamParticles.length;
    for (int i = 0; i < slamParticles.length; i++) {
      double vx = SlamRandomUtil.getGaussian(linVelAvg, linVelStd);
      if (vx < 0)
        vx = 0;
      double vangle = SlamRandomUtil.getGaussian(0, angVelStd);
      slamParticles[i].initialize(pose, RealScalar.of(vx), RealScalar.of(vangle), initLikelihood);
    }
  }

  public static void updateLikelihoods(SlamParticle[] slamParticles, MapProvider map, double[] gokartFramePos, double alpha) {
    double sumOfLikelihoods = 0;
    for (int i = 0; i < slamParticles.length; i++) {
      // map go kart coordinates into world coordinates using the state estimate of the particle
      Tensor worldCoord = slamParticles[i].getGeometricLayer().toVector(gokartFramePos[0], gokartFramePos[1]);
      // get the likelihoodMap value of the computed world coordinate position and apply the actual update rule
      double updatedParticleLikelihood = slamParticles[i].getParticleLikelihood() + 2 * alpha * map.getValue(worldCoord) / map.getMaxValue();
      slamParticles[i].setParticleLikelihood(updatedParticleLikelihood);
      sumOfLikelihoods += updatedParticleLikelihood;
    }
    // normalize particle likelihoods
    for (int i = 0; i < slamParticles.length; i++)
      slamParticles[i].setParticleLikelihood(slamParticles[i].getParticleLikelihood() / sumOfLikelihoods);
  }

  /** propagate the particles' state estimates
   * 
   * @param slamParticles
   * @param dT [s] */
  public static void propagateStateEstimate(SlamParticle[] slamParticles, double dT) {
    for (int i = 0; i < slamParticles.length; i++)
      slamParticles[i].propagateStateEstimate(dT);
  }

  /** resamples the particles
   * 
   * @param slamParticles */
  public static void resampleParticles(SlamParticle[] slamParticles, double dT) {
    // different methods to assign particles can be tested here
    // SlamParticleUtil.multinomialSampling(slamParticles);
    SlamParticleUtil.neglectLowLikelihoodds(slamParticles);
    // depending on resampling method, roughening might be used
    SlamParticleUtil.particleRoughening(slamParticles, dT);
  }

  private static void multinomialSampling(SlamParticle[] slamParticles) {
    int numbOfPart = slamParticles.length;
    // assigned particle numbers start at zero
    int[] assignedPart = new int[slamParticles.length];
    // generate array with cumulative particle probabilities
    double[] particleCDF = new double[numbOfPart];
    for (int i = 1; i < numbOfPart; i++) {
      particleCDF[i] = particleCDF[i - 1] + slamParticles[i].getParticleLikelihood();
    }
    // draw as many random numbers as particles and find corresponding CDF number
    double[] randomNumbers = new double[numbOfPart];
    SlamRandomUtil.setUniformRVArray(randomNumbers);
    for (int i = 0; i < numbOfPart; i++) {
      for (int j = 1; j < numbOfPart; j++) {
        if (randomNumbers[i] <= particleCDF[j]) {
          assignedPart[i] = j - 1;
          break;
        }
        assignedPart[i] = numbOfPart - 1;
      }
    }
    // set state once assignedPart array is determined
    double initLikelihood = (double) 1 / slamParticles.length;
    for (int i = 0; i < slamParticles.length; i++) {
      slamParticles[i].setStateFromParticle(slamParticles[assignedPart[i]], initLikelihood);
    }
  }

  // just throw away lowest likelihoods
  private static void neglectLowLikelihoodds(SlamParticle[] slamParticles) {
    double initLikelihood = (double) 1 / slamParticles.length;
    int neglectNumber = slamParticles.length / 3;
    // sort by likelihood
    Arrays.sort(slamParticles, SlamCompare);
    // System.out.println("******");
    // for (int i = 0; i < slamParticles.length; i++) {
    // System.out.println(slamParticles[i].getParticleLikelihood());
    // }
    int startIndex = slamParticles.length - neglectNumber;
    for (int i = startIndex; i < slamParticles.length; i++) {
      slamParticles[i].setStateFromParticle(slamParticles[i - startIndex], initLikelihood);
    }
  }

  // roughens the linVel and angVel of the particles
  // TODO maybe roughen position as well? or only position
  // TODO parameterize it
  private static void particleRoughening(SlamParticle[] slamParticles, double dT) {
    for (int i = 0; i < slamParticles.length; i++) {
      double vx = slamParticles[i].getLinVelDouble() + SlamRandomUtil.getGaussian(0, 0.3);
      if (vx < 0)
        vx = 0;
      double vangle = slamParticles[i].getAngVelDouble() + SlamRandomUtil.getGaussian(0, 0.1);
      slamParticles[i].setLinVel(RealScalar.of(vx));
      // TODO angVel limits should be incorporated
      slamParticles[i].setAngVel(RealScalar.of(vangle));
    }
  }

  // get average pose of particles with highest likelihood
  public static Tensor getAveragePose(SlamParticle[] slamParticles, int relevantRange) {
    Tensor expectedPose = Tensors.of(Quantity.of(0, SI.METER), Quantity.of(0, SI.METER), DoubleScalar.of(0));
    Arrays.sort(slamParticles, 0, relevantRange, SlamCompare);
    double likelihoodSum = 0;
    for (int i = 0; i < relevantRange; i++) {
      Tensor pose = slamParticles[i].getPose();
      double likelihood = slamParticles[i].getParticleLikelihood();
      likelihoodSum += likelihood;
      expectedPose = expectedPose.add(pose.multiply(RealScalar.of(likelihood)));
    }
    return expectedPose.divide(RealScalar.of(likelihoodSum));
  }

  // get maximum likelihood pose
  // TODO compare with getAveragePose(slamParticles, 1) which could replace it
  public static Tensor getMLPose(SlamParticle[] slamParticles) {
    double maxLikelihood = 0;
    int maxLikelihoodIndex = 0;
    for (int i = 0; i < slamParticles.length; i++) {
      if (slamParticles[i].getParticleLikelihood() > maxLikelihood) {
        maxLikelihood = slamParticles[i].getParticleLikelihood();
        maxLikelihoodIndex = i;
      }
    }
    return slamParticles[maxLikelihoodIndex].getPose();
  }
}
