package edu.umich.clark.grizzhacks2018;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.pm.*;
import android.view.View;
import android.widget.Button;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.CropHint;
import com.google.cloud.vision.v1.CropHintsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageContext;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity
{
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String mCurrentPhotoPath;
    private Button mCameraButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        mCameraButton = findViewById((R.id.cameraButton));
        mCameraButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Code here executes on main thread after user presses button
                try
                {
                    createImageFile();
                }
                catch(IOException e)
                {
                    // handle error
                }
                dispatchTakePictureIntent();
                Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
            }
        });
    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
        {
            // Create the File where the photo should go
            File photoFile = null;
            try
            {
                photoFile = createImageFile();
            } catch (IOException ex)
            {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null)
            {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "edu.umich.clark.grizzhacks2018.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    public static void detectFeatures(String fileName, PrintStream out) throws Exception, IOException
    {

        // Reads the image file into memory
        Path path = Paths.get(fileName);
        byte[] data = Files.readAllBytes(path);
        ByteString imgBytes = ByteString.copyFrom(data);
        List<AnnotateImageRequest> requestArray = new ArrayList<>();

        Feature cropHints = Feature.newBuilder().setType(Type.CROP_HINTS).build();
        Feature labels = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
        Feature textDetection = Feature.newBuilder().setType(Type.DOCUMENT_TEXT_DETECTION).build();
        Image img = Image.newBuilder().setContent(imgBytes).build();
        ImageContext imageContext = ImageContext.newBuilder().addLanguageHints("en-t-i0-handwrit").build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(cropHints)
                .addFeatures(labels)
                .addFeatures(textDetection)
                .setImage(img)
                .setImageContext(imageContext)
                .build();
        requestArray.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create())
        {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requestArray);
            List<AnnotateImageResponse> responseArray = response.getResponsesList();
            client.close();
            for (AnnotateImageResponse res : responseArray)
            {
                if (res.hasError())
                {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                CropHintsAnnotation cAnnotation = res.getCropHintsAnnotation();
                for (CropHint hint : cAnnotation.getCropHintsList())
                {
                    out.println(hint.getBoundingPoly());
                }

                for (EntityAnnotation eAnnotation : res.getLabelAnnotationsList())
                {
                    eAnnotation.getAllFields().forEach((k, v) -> out.printf("%s : %s\n", k, v.toString()));
                }

                TextAnnotation annotation = res.getFullTextAnnotation();
                for (Page page : annotation.getPagesList())
                {
                    String pageText = "";
                    for (Block block : page.getBlocksList())
                    {
                        String blockText = "";
                        for (Paragraph para : block.getParagraphsList())
                        {
                            String paraText = "";
                            for (Word word : para.getWordsList())
                            {
                                String wordText = "";
                                for (Symbol symbol : word.getSymbolsList())
                                {
                                    wordText = wordText + symbol.getText();
                                    out.format(
                                            "Symbol text: %s (confidence: %f)\n",
                                            symbol.getText(), symbol.getConfidence());
                                }
                                out.format("Word text: %s (confidence: %f)\n\n", wordText, word.getConfidence());
                                paraText = String.format("%s %s", paraText, wordText);
                            }
                            // Output Example using Paragraph:
                            out.println("\nParagraph: \n" + paraText);
                            out.format("Paragraph Confidence: %f\n", para.getConfidence());
                            blockText = blockText + paraText;
                        }
                        pageText = pageText + blockText;
                    }
                }
                out.println("\nComplete annotation:");
                out.println(annotation.getText());
            }
        }
    }
}
