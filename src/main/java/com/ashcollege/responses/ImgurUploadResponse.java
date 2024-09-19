package com.ashcollege.responses;

import com.ashcollege.entities.ImgurImageDataModel;

public class ImgurUploadResponse {
    private int status;
    private boolean success;
    private ImgurImageDataModel data;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ImgurImageDataModel getData() {
        return data;
    }

    public void setData(ImgurImageDataModel data) {
        this.data = data;
    }
}