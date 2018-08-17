// code by mg
package ch.ethz.idsc.demo.mg.slam.vis;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.idsc.demo.mg.slam.SlamConfig;
import ch.ethz.idsc.demo.mg.slam.SlamFileLocations;
import ch.ethz.idsc.demo.mg.slam.algo.SlamProvider;
import ch.ethz.idsc.demo.mg.util.vis.VisGeneralUtil;
import ch.ethz.idsc.gokart.core.pos.GokartPoseInterface;
import ch.ethz.idsc.retina.dev.davis.DavisDvsListener;
import ch.ethz.idsc.retina.dev.davis._240c.DavisDvsEvent;
import ch.ethz.idsc.retina.util.math.Magnitude;

/** wrapper class for the SLAM visualization */
public class SlamViewer implements DavisDvsListener {
  private final GokartPoseInterface gokartLidarPose;
  private final SlamProvider slamProvider;
  private final SlamMapGUI slamMapGUI;
  private final SlamMapFrame[] slamMapFrames;
  private final String logFilename;
  private final File parentFilePath;
  private final boolean lidarMappingMode;
  private final boolean saveSlamFrame;
  private final double visualizationInterval;
  private final double savingInterval;
  private double lastImagingTimeStamp;
  private double lastSavingTimeStamp;
  private int imageCount;
  // ---
  private final TimerTask visualizationTask;
  private final TimerTask saveFrameTask;

  public SlamViewer(SlamConfig slamConfig, SlamProvider slamProvider, GokartPoseInterface gokartLidarPose, Timer timer) {
    this.gokartLidarPose = gokartLidarPose;
    this.slamProvider = slamProvider;
    logFilename = slamConfig.davisConfig.logFilename();
    parentFilePath = SlamFileLocations.mapFrames(logFilename);
    lidarMappingMode = slamConfig.lidarMappingMode;
    saveSlamFrame = slamConfig.saveSlamFrame;
    visualizationInterval = Magnitude.SECOND.toDouble(slamConfig._visualizationInterval);
    savingInterval = Magnitude.SECOND.toDouble(slamConfig._savingInterval);
    slamMapGUI = new SlamMapGUI(slamConfig);
    slamMapFrames = new SlamMapFrame[3];
    for (int i = 0; i < slamMapFrames.length; i++)
      slamMapFrames[i] = new SlamMapFrame(slamConfig);
    visualizationTask = new TimerTask() {
      @Override
      public void run() {
        if (slamProvider.getIsInitialized())
          slamMapGUI.setFrames(StaticHelper.constructFrames(slamMapFrames, slamProvider, gokartLidarPose, lidarMappingMode));
      }
    };
    saveFrameTask = new TimerTask() {
      @Override
      public void run() {
        if (saveSlamFrame && slamProvider.getIsInitialized())
          saveFrame();
      }
    };
    timer.schedule(visualizationTask, 0, Magnitude.MILLI_SECOND.toLong(slamConfig._visualizationInterval));
    timer.schedule(saveFrameTask, 0, Magnitude.MILLI_SECOND.toLong(slamConfig._savingInterval));
  }

  @Override // from DavisDvsListener
  public void davisDvs(DavisDvsEvent davisDvsEvent) {
    // cancel timertasks since we are in offline mode
    visualizationTask.cancel();
    saveFrameTask.cancel();
    if (slamProvider.getIsInitialized()) {
      double timeStamp = davisDvsEvent.time * 1E-6;
      if (timeStamp - lastImagingTimeStamp > visualizationInterval) {
        slamMapGUI.setFrames(StaticHelper.constructFrames(slamMapFrames, slamProvider, gokartLidarPose, lidarMappingMode));
        lastImagingTimeStamp = timeStamp;
      }
      if (saveSlamFrame && (timeStamp - lastSavingTimeStamp > savingInterval)) {
        saveFrame(timeStamp);
        lastSavingTimeStamp = timeStamp;
      }
    }
  }

  private void saveFrame(double timeStamp) {
    imageCount++;
    BufferedImage slamFrame = StaticHelper.constructFrames(slamMapFrames, slamProvider, gokartLidarPose, lidarMappingMode)[1];
    VisGeneralUtil.saveFrame(slamFrame, parentFilePath, logFilename, timeStamp, imageCount);
  }

  private void saveFrame() {
    imageCount++;
    BufferedImage slamFrame = StaticHelper.constructFrames(slamMapFrames, slamProvider, gokartLidarPose, lidarMappingMode)[1];
    VisGeneralUtil.saveFrame(slamFrame, parentFilePath, logFilename, imageCount);
  }
}