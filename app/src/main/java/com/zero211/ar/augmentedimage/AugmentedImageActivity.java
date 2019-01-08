/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zero211.ar.augmentedimage;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
public class AugmentedImageActivity extends AppCompatActivity
{
    /*
            TODO: Modify code to download image/video combos
            from an HTTPS reachable source.  Also modify the Augmented image db init code
            to insert the downloaded images into the db at runtime.
            Make this download action a background synchronization task.
            Maintain at least 1 built-in image and video combo for default, off-network running.
     */
    private static final String SHAWSHANK_IMG_NAME = "shawshank_2x3";
    private static final String RITA_IMG_NAME = "rita_2x3";
    private static final String MARILYN_IMG_NAME = "marilyn_2x3";
    private static final String RAQUEL_IMG_NAME = "raquel_2x3";

    private static final HashMap<String, Integer> imgNameToMovieID = new HashMap<>();

    {
        imgNameToMovieID.put(SHAWSHANK_IMG_NAME, R.raw.shawshank);
        imgNameToMovieID.put(RITA_IMG_NAME, R.raw.rita);
        imgNameToMovieID.put(MARILYN_IMG_NAME, R.raw.marilyn);
        imgNameToMovieID.put(RAQUEL_IMG_NAME, R.raw.raquel);
    }

    private ArFragment arFragment;
    private ImageView fitToScanView;
    private long lastFrameTime;

    /* Augmented image and its associated center pose anchor, keyed by the augmented image in
    the database.  This Map tracks which augmented images already have an associated AugmentedImageVideoNode
    created and attached to the top-level Scene.
    */
    private final Map<String, AugmentedImageVideoNode> augmentedImageMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        fitToScanView = findViewById(R.id.image_view_fit_to_scan);

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (augmentedImageMap.isEmpty())
        {
            fitToScanView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Registered with the Sceneform Scene object, this method is called at the start of each frame.
     *
     * @param frameTime - time since last frame.
     */
    private void onUpdateFrame(FrameTime frameTime)
    {
        Frame frame = arFragment.getArSceneView().getArFrame();

        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING || lastFrameTime == frame.getTimestamp())
        {
            return;
        }

        lastFrameTime = frame.getTimestamp();

        Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : updatedAugmentedImages)
        {
            AugmentedImageVideoNode augmentedImageVideoNode = augmentedImageMap.get(augmentedImage.getName());
            Integer movieID = imgNameToMovieID.get(augmentedImage.getName());

            switch (augmentedImage.getTrackingState())
            {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    Toast.makeText(this, "Detected Image : " + augmentedImage.getName(), Toast.LENGTH_SHORT).show();
                    break;

                case TRACKING:
                    // Create a new anchor for newly found images.

                        if (augmentedImageVideoNode == null)
                        {
                            fitToScanView.setVisibility(View.GONE);
                            Toast.makeText(this, "Tracking Image : " + augmentedImage.getName(), Toast.LENGTH_SHORT).show();
//                        AugmentedImageNode node = new AugmentedImageNode(this);
//                        node.setImage(augmentedImage);
                            if (movieID != null)
                            {
                                AugmentedImageVideoNode node = new AugmentedImageVideoNode(this, augmentedImage, movieID);
                                augmentedImageMap.put(augmentedImage.getName(), node);
                                arFragment.getArSceneView().getScene().addChild(node);
                            }
                            else
                            {
                                Toast.makeText(this, "Couldn't find the movie ID for : " + augmentedImage.getName(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            augmentedImageVideoNode.update(augmentedImage);
                        }

                    break;

                case STOPPED:
                    if (augmentedImageVideoNode != null)
                    {
                        Toast.makeText(this, "Stopped tracking : " + augmentedImage.getName(), Toast.LENGTH_SHORT).show();
                        arFragment.getArSceneView().getScene().removeChild(augmentedImageVideoNode);
                        augmentedImageVideoNode.stopAndRelease();
                        augmentedImageMap.remove(augmentedImage.getName());
                        fitToScanView.setVisibility(View.VISIBLE);
                    }

                    break;

            }
        }
    }
}
