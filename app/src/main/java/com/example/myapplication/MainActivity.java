package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.myapplication.ml.ConvertedModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;



public class MainActivity extends AppCompatActivity {

    Button camera, gallery;
    ImageView imageView;
    TextView result, confidence;
    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);

        confidence = findViewById(R.id.confidence);
        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent, 1);
            }
        });
    }

    public void classifyImage(Bitmap image){
        try {
            ConvertedModel model = ConvertedModel.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for(int i = 0; i < imageSize; i ++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            ConvertedModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Aksamitówka złota (Phaeolepiota aurea)", "Boczniak ostrygowaty (Oleorotus ostreatus)", "Borowik purpurowy (Rubroboletus rhodocanthus)", "Borowik szatański (Rubroboletus satanas)", "Borowik szlachetny (Boletus edulis)", "Czernidłak kołpakowaty (Coprinus comatus)", "Czernidłak pstry (Coprinopsis picacea)", "Czubajka kania (Macrolepiota procera)", "Gaska zielona (Tricholoma equestre)", "Gołąbek krwisty (Russula sanguinea)", "Gąska biaława (Tricholoma album)", "Gąska czarnołuskowa (Tricholoma atrosquamosum)", "Gąska niekształtna (Tricholoma portentosum)", "Gąska siarkowa (Tricholoma sulphureum)", "Kozlarz babka (Leccinum scabrum)", "Koźlarz czerwony (Leccinum aurantiacum)", "Krowiak Olszowy (Paxillus rubicundulus)", "Kępkowiec białawy (Leucocybe connata)", "Luskwiak rdzwoluskowy (Pholiota squarrosa)", "Maslak ziarnisty (Suillus_granulatus)", "Maslak zwyczajny (Suillus_luteus)", "Maślak pstry (Suillus variegatus)", "Maślanka wiązkowa (Hypholoma fasciculare)", "Mleczaj rudy (Lactarius_rufus)", "Mleczaj rydz (Lactarius_deliciosus)", "Muchomor Zielonawy (Amanita phalloides)", "Muchomor czerwony (Amanita muscaria)", "Muchomor jadowity (Amanita virosa)", "Muchomor porfirowy (Amanita porphyria)", "Niegrzyb", "Pieczarka leśna (Agaricus silvaticus)", "Pieczarka łąkowa (Agaricus campestris)", "Pieprznik jadalny (Cantharellus_cibarius)", "Piestrzenica kasztanowata (Guromitra esculenta)", "Podgrzybek brunatny (Imleria_badia)", "Strzępiak ceglasty (Inocybe erubescens)", "Wilgotnica szkarłatna (Hygrocybe coccinea)", "Zasloniak filetowy (Cortinarius_violaceus)", "Zasłonak rudy (Cortinarius orellanus)", "Zasłoniak cynamonowy (Cortinarius_cinnamomeus)", "Ziarnówka Ochrowożółta (Cystoderma amianthinum)"};


            result.setText(classes[maxPos]);

            // display the confidence in percentage
            maxConfidence = maxConfidence * 100;
            String stringConfidence=String.valueOf(maxConfidence);
            stringConfidence = stringConfidence.substring(0, 2);
            stringConfidence = stringConfidence + " %";

            confidence.setText(stringConfidence);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == 3){
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }else{
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
