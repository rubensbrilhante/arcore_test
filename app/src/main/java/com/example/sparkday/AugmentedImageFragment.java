package com.example.sparkday;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;

public class AugmentedImageFragment extends ArFragment {

    private static final String IMAGE_DATABASE = "sparkday2.imgdb";
    AugmentedImageDatabase augmentedImageDatabase;
    ArListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        // Turn off the plane discovery since we're only looking for images
        getPlaneDiscoveryController().hide();
        getPlaneDiscoveryController().setInstructionView(null);
        getArSceneView().getPlaneRenderer().setEnabled(false);
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof ArListener) {
            listener = (ArListener) context;
        } else {
            throw new RuntimeException("Missing ArListener");
        }
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        config.setFocusMode(Config.FocusMode.AUTO);
        try (InputStream is = getContext().getAssets().open(IMAGE_DATABASE)) {
            augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
            config.setAugmentedImageDatabase(augmentedImageDatabase);
        } catch (IOException e) {
            listener.onError(new Exception("Faild to load DB", e));
        }

        return config;
    }

    public interface ArListener {

        void onError(Exception e);
    }
}
