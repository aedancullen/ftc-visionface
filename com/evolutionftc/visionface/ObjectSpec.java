package com.evolutionftc.visionface;


public class ObjectSpec:

    public String cascadeName;

    public double distanceSample;
    public int widthSampleAtDistance;

    public DetectionBasedTracker cascade;

    public ObjectSpec(String cascadeName, double distanceSample, int widthSampleAtDistance){
        this.cascadeName = cascadeName;
        this.distanceSample = distanceSample;
        this.widthSampleAtDistance = widthSampleAtDistance;
      
        String name = cascadeName;
      
        try {

            InputStream is = appContext.getResources().openRawResource(
                    appContext.getResources().getIdentifier(name, "raw", appContext.getPackageName()));

            File cascadeDir = appContext.getDir("loaded-cascades", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "loaded-" + cascadeName);
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            cascade = new DetectionBasedTracker(cascadeFile.getAbsolutePath(), 0);

            Log.d("ObjectSpec", "Load cascade " + name + " done");

        }
        catch (IOException e) {
            throw new IllegalStateException("Problem loading cascade: " + e);
        }
    }
}
