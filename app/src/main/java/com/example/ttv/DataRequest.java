package com.example.ttv;

import com.google.gson.annotations.SerializedName;

    public class DataRequest {
        @SerializedName("summary")
        private String textData;

        // Ảnh dưới dạng base64 string
        @SerializedName("image_data")
        private String imageData;

        public DataRequest(String textData, String imageData) {
            this.textData = textData;
            this.imageData = imageData;
        }

        public String getTextData() {
            return textData;
        }

        public void setTextData(String textData) {
            this.textData = textData;
        }

        public String getImageData() {
            return imageData;
        }

        public void setImageData(String imageData) {
            this.imageData = imageData;
        }


}
