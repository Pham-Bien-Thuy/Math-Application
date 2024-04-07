package com.example.ttv.ocr_model;

import com.googlecode.tesseract.android.TessBaseAPI;

public class Config {

	public static final int TESS_ENGINE = TessBaseAPI.OEM_LSTM_ONLY;

	public static final String TESS_LANG = "eng";

	public static final String IMAGE_NAME = "img_1.png";
}
