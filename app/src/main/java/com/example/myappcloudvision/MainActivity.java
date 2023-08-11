package com.example.myappcloudvision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.TextAnnotation;
import com.google.api.services.vision.v1.model.Vertex;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Vision vision;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //API KEY: AIzaSyBvmMaKjamt8worxAoAgWHE_GYqIMlvoeE

        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyDMmRXHBYOjJyXZruXemR11tl7uiJ2T_Q8"));
        vision = visionBuilder.build();
    }
    private static int RESULT_LOAD_IMAGE = 1;
    public void OpenGallery(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data)
        {
            ImageView imageView = (ImageView) findViewById(R.id.imagen);
            imageView.setImageURI(data.getData());
        }
    }
    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public Image getImageToProcess(){
        ImageView imagen = (ImageView)findViewById(R.id.imagen);
        BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
       //bitmap = scaleBitmapDown(bitmap, 800);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageInByte = stream.toByteArray();
        Image inputImage = new Image();
        inputImage.encodeContent(imageInByte);
        return inputImage;
    }
    //subir a google
    //varias imagenes y varias solucitudes
    public BatchAnnotateImagesRequest setBatchRequest(String TipoSolic, Image inputImage){
        Feature desiredFeature = new Feature();
        desiredFeature.setType(TipoSolic);
        AnnotateImageRequest request = new AnnotateImageRequest();
        request.setImage(inputImage);
        request.setFeatures(Arrays.asList(desiredFeature));
        BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
        //aqui se puede enviar un vector de request
        batchRequest.setRequests(Arrays.asList(request));
        return batchRequest;
    }

        //la peticion de la API debe estar dentro de un hilo para que no se congele el celular y pueda seguir procesando
    //Procesar de imagen a textp
    public void ProcesarTexto(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest =
                        setBatchRequest("TEXT_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();

                    final TextAnnotation text = response.getResponses().get(0).getFullTextAnnotation();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.textView2);
                            imageDetail.setText(text.getText());
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //Procesar img  J0der Maholy aun no te olvido
    //SE ENVIA CON LABEL_DETECTION PEDAZO DE PENDEJO SUBNORAML DE MIERDA
    public void ProcesarImagen(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest =
                        setBatchRequest("LABEL_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();

                    final StringBuilder message = new StringBuilder("Se ha encontrado los siguientes Objetos:\n\n");
                    List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
                    if (labels != null) {
                        for (EntityAnnotation label : labels)
                            message.append(String.format(Locale.US, "%.2f: %s\n", label.getScore()*100, label.getDescription()));
                    } else {
                        message.append("No hay ningún Objeto");
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.textView2);
                            imageDetail.setText(message.toString());
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //Procesar rostro
    public void ProcesarRosto(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest =
                        setBatchRequest("FACE_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();

                    List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
                    int numberOfFaces = faces.size();
                    ImageView imagen = (ImageView)findViewById(R.id.imagen);
                    BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
                    Bitmap bitmap = drawable.getBitmap().copy(Bitmap.Config.ARGB_8888,true);
                   // bitmap = scaleBitmapDown(bitmap, 800);
                    Canvas canvas = new Canvas(bitmap);
                    String likelihoods = "";
                    FaceAnnotation caras;
                    Vertex vertice1,vertice2;
                    Paint paint = new Paint();
                    RectF rectangulo;
                    //tamano del width de los puntos
                    paint.setStrokeWidth(10);
                    // 244, 208, 63
                    paint.setColor(Color.rgb(20, 90, 50));
                    for(int i=0; i<numberOfFaces; i++){
                        caras=faces.get(i);
                        rectangulo = new RectF();
                        int j ;
                        for (j=0; j<caras.getBoundingPoly().getVertices().size();j++){
                            if (j>0){
                                vertice1 = caras.getBoundingPoly().getVertices().get(j);
                                vertice2 = caras.getBoundingPoly().getVertices().get(j-1);
                                if(vertice1.getX()!=null && vertice1.getY()!= null && vertice2.getX()!=null && vertice2.getY()!= null) {
                                    canvas.drawLine(vertice1.getX(), vertice1.getY(), vertice2.getX(), vertice2.getY(), paint);
                                }
                            }
                        }
                        vertice1 = caras.getBoundingPoly().getVertices().get(j-1);
                        vertice2 = caras.getBoundingPoly().getVertices().get(0);
                        if(vertice1.getX()!=null && vertice1.getY()!= null && vertice2.getX()!=null && vertice2.getY()!= null) {
                            canvas.drawLine(vertice1.getX(), vertice1.getY(), vertice2.getX(), vertice2.getY(), paint);
                        }

                        /*
                        rectangulo.top = caras.getBoundingPoly().getVertices().get(0).getX();
                        rectangulo.bottom = caras.getBoundingPoly().getVertices().get(2).getX();
                        rectangulo.left = caras.getBoundingPoly().getVertices().get(0).getX();
                        rectangulo.right = caras.getBoundingPoly().getVertices().get(1).getX();
                        /*


                         */

                        /*
                        for (int j=0; j<caras.getBoundingPoly().getVertices().size();j++){
                            vertice = caras.getBoundingPoly().getVertices().get(j);
                            if (vertice.getY() != null && vertice.getX()!=null) {
                                canvas.drawPoint(caras.getBoundingPoly().getVertices().get(j).getX(),caras.getBoundingPoly().getVertices().get(j).getY(), paint);
                            }
                        }

                         */
                        //para poner texto
                        paint.setTextSize(40);
                        //paint.setColor(Color.rgb( 253,  254,  254));
                        //caras.getUnknowKey();
                        //caras.getUnknownKeys().get("sex").toString()


                        //canvas.drawText("Tu mama ", vertice2.getX(),vertice1.getY()+40,paint);
                        canvas.drawText(caras.getSorrowLikelihood().toString(), vertice2.getX(),vertice1.getY()+40,paint);

                        likelihoods += "\n Rostro " + i + " " + faces.get(i).getJoyLikelihood();

                    }
                    imagen.setImageBitmap(bitmap);
                    final String message = "Esta imagen tiene " + numberOfFaces + " rostros " + likelihoods;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.textView2);
                            imageDetail.setText(message.toString());
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}