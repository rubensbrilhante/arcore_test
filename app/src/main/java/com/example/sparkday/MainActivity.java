package com.example.sparkday;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener, AugmentedImageFragment.ArListener {

    private TextView statusLabel;
    private ArFragment arFragment;
    private Map<String, Node> augmentedImageMap = new HashMap<>();
    private static CompletableFuture<ModelRenderable> renderable;
    private static ModelRenderable planeRenderable;
    private boolean hasError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusLabel = findViewById(R.id.status);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sf_fragment);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
        renderable = ModelRenderable.builder()
                .setSource(this, Uri.parse("Mesh_Cat.sfb"))
                .build();


        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.WHITE))
                .thenAccept(
                        material -> {
                            planeRenderable =
                                    ShapeFactory.makeCube(new Vector3(1f, 0.0001f, 1), new Vector3(0, 0, 0), material);
                            material.setBoolean("colorWrite", false);
//                            rectRenderable = ShapeFactory.makeCube(size, center, material);
                            planeRenderable.setRenderPriority(7);
                        });

    }


    @Override
    public void onUpdate(FrameTime frameTime) {
        if (hasError) return;

        Frame frame = arFragment.getArSceneView().getArFrame();

        // If there is no frame, just return.
        if (frame == null) {
            statusLabel.setText("No frame.");
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);
        if (updatedAugmentedImages.size() == 0) {
            statusLabel.setText("No images.");
            return;
        }
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case TRACKING:
                    if (!augmentedImageMap.containsKey(augmentedImage.getName())) {
                        insertNode(augmentedImage);
                        statusLabel.setText("TRACKING: ADDING NODE");
                    } else {
                        statusLabel.setText("TRACKING: NODE ADDED");
                    }
                    break;
                case PAUSED:
                    statusLabel.setText("WAITING TRACK: " + augmentedImage.getName());
                    break;
                case STOPPED:
                    statusLabel.setText("STOPPED");
                    Node node = augmentedImageMap.get(augmentedImage.getName());
                    augmentedImageMap.remove(augmentedImage);
                    arFragment.getArSceneView().getScene().removeChild(node);
                    break;
            }
        }
    }

    private void insertNode(AugmentedImage augmentedImage) {
        if (!renderable.isDone()) {
            Log.d("_debug", "!renderable.isDone()");
            CompletableFuture.allOf(renderable)
                    .thenAccept((Void aVoid) -> insertNode(augmentedImage))
                    .exceptionally(
                            throwable -> {
                                onError(new Exception("Fail to load renderable", throwable));
                                return null;
                            });
        } else {
            Log.d("_debug", "renderable.isDone()");
            final float objSize = 100f;
            final float biggestImageSize = Math.max(augmentedImage.getExtentX(), augmentedImage.getExtentZ());
            float scale = biggestImageSize / objSize;

            AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));

            Node node = new Node();
            node.setParent(anchorNode);
            node.setRenderable(renderable.getNow(null));
            node.setLocalScale(new Vector3(scale, scale, scale));
            node.setLocalPosition(new Vector3(0, 0f, 0));
//
            Node planeNode = new Node();
            planeNode.setParent(anchorNode);
            planeNode.setLocalScale(new Vector3(augmentedImage.getExtentX(), 1f, augmentedImage.getExtentZ()));
            planeNode.setRenderable(planeRenderable);
            planeNode.setLocalPosition(new Vector3(0, 0f, 0));

            arFragment.getArSceneView().getScene().addChild(anchorNode);
            augmentedImageMap.put(augmentedImage.getName(), anchorNode);
        }
    }

    @Override
    public void onError(Exception e) {
        hasError = true;
        arFragment.getArSceneView().getScene().removeOnUpdateListener(this);
        statusLabel.setText("Error: " + e.getMessage());
    }
}
