package com.content_generation_service.generation.service.visual;

public interface ProgressListener {
    void onProgress(double percentage);
    void onComplete();
    void onError();
}
