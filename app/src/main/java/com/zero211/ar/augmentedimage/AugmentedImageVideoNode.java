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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.io.IOException;


@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageVideoNode extends AnchorNode
{

    private static final String TAG = "AugmentedImageVideoNode";

    // The color to filter out of the video.
    private static final Color CHROMA_KEY_COLOR = new Color(0.1843f, 1.0f, 0.098f);

    private AugmentedImage image;
    private Context context;

    @Nullable
    private Node videoNode;
    private ModelRenderable videoRenderable;
    private MediaPlayer mediaPlayer;


    // Controls the height of the video in world space.
    private static final float VIDEO_HEIGHT_METERS = 2.0f;

    public AugmentedImageVideoNode(Context context, AugmentedImage image, Integer videoUri)
    {
        this.context = context;
        this.image = image;

        // Create an ExternalTexture for displaying the contents of the video.
        ExternalTexture texture = new ExternalTexture();

        // Create an Android MediaPlayer to capture the video on the external texture's surface.


        mediaPlayer = MediaPlayer.create(context, videoUri);
        mediaPlayer.setSurface(texture.getSurface());
        mediaPlayer.setLooping(false);

        ModelRenderable.builder()
                .setSource(context, Uri.parse("models/chroma_key_video.sfb"))
                .build()
            .thenAccept(
                    renderable -> {
                        videoRenderable = renderable;
                        renderable.getMaterial().setExternalTexture("videoTexture", texture);
                        renderable.getMaterial().setFloat4("keyColor", CHROMA_KEY_COLOR);
                    })
            .exceptionally(
                    throwable -> {
                        Toast toast =
                                Toast.makeText(context, "Unable to load video renderable", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return null;
                    });

        // Set the anchor based on the center of the image.
        setAnchor(image.createAnchor(image.getCenterPose()));

        videoNode = new Node();
        videoNode.setParent(this);

        Quaternion videoRotation = Quaternion.axisAngle(new Vector3(1f, 0, 0), -90f);
        videoNode.setLocalRotation(videoRotation);


        this.update(image);

        this.setOnTapListener(new OnTapListener()
        {
            @Override
            public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent)
            {

                if (!mediaPlayer.isPlaying())
                {
                    Toast.makeText(context, "Starting/Restarting mediaPlayer", Toast.LENGTH_SHORT).show();
                    //mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                }
                else
                {
                    Toast.makeText(context, "Stopping mediaPlayer", Toast.LENGTH_SHORT).show();
                    mediaPlayer.pause();
                }
            }
        });



        // Start playing the video when the first node is placed.
        if (!mediaPlayer.isPlaying())
        {
            mediaPlayer.start();

            // Wait to set the renderable until the first frame of the  video becomes available.
            // This prevents the renderable from briefly appearing as a black quad before the video
            // plays.
            texture
                    .getSurfaceTexture()
                    .setOnFrameAvailableListener(
                            (SurfaceTexture surfaceTexture) -> {
                                videoNode.setRenderable(videoRenderable);
                                texture.getSurfaceTexture().setOnFrameAvailableListener(null);
                            });
        }
        else
        {
            videoNode.setRenderable(videoRenderable);
        }

    }

    public void update(AugmentedImage image)
    {

        // Set the scale of the node so that the aspect ratio of the video is correct.
        float videoWidth = mediaPlayer.getVideoWidth();
        float videoHeight = mediaPlayer.getVideoHeight();

        //Vector3 videoScale = new Vector3(VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 0.0f);
        //Vector3 videoScale = new Vector3(image.getExtentZ() * (videoWidth / videoHeight), image.getExtentZ(), 1.0f);
        //Vector3 videoScale = new Vector3(image.getExtentX(), image.getExtentZ(), 0.001f);
        Vector3 videoScale = new Vector3(image.getExtentX(), image.getExtentX() * (videoHeight / videoWidth), 0.001f);
        videoNode.setWorldScale(videoScale);

        Vector3 imagePos = this.getWorldPosition();

        //videoNode.setWorldPosition(imagePos);
        //videoNode.setWorldPosition(new Vector3(imagePos.x, imagePos.y , imagePos.z ));
        //videoNode.setWorldPosition(new Vector3(imagePos.x, imagePos.y, imagePos.z + (image.getExtentZ() * 0.5f)));
        //videoNode.setWorldPosition(new Vector3(imagePos.x, imagePos.y + (image.getExtentZ() * -0.5f), imagePos.z ));
        videoNode.setWorldPosition(new Vector3(imagePos.x, imagePos.y + (videoScale.y * -0.5f), imagePos.z ));
    }

    public void stopAndRelease()
    {
        Toast.makeText(context , "Stop and Release called", Toast.LENGTH_SHORT).show();
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public AugmentedImage getImage()
    {
        return image;
    }
}
