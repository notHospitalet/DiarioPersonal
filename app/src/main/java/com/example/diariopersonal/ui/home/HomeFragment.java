package com.example.diariopersonal.ui.home;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.diariopersonal.R;
import com.example.diariopersonal.databinding.FragmentHomeBinding;
import com.example.diariopersonal.ui.Incidencia;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private FirebaseUser authUser;
    String mCurrentPhotoPath; // almacena la ruta de la foto
    private Uri photoURI; // almacena la URI de la foto
    private ImageView foto; // muestra la foto hecha
    static final int REQUEST_TAKE_PHOTO = 1;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // se infla el layout
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        HomeViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // se observa el 'livedata' que contiene la direccion actual con la direccion y la hora actual
        HomeViewModel.getCurrentAddress().observe(getViewLifecycleOwner(), address -> {
            binding.txtDireccio.setText(String.format(
                    "Direcció: %1$s \n Hora: %2$tr",
                    address, System.currentTimeMillis())
            );
        });

        // se observa el 'livedata' que contiene la latitud y la longitud actual
        sharedViewModel.getCurrentLatLng().observe(getViewLifecycleOwner(), latlng -> {
           binding.txtLatitud.setText(String.valueOf(latlng.latitude));
            binding.txtLongitud.setText(String.valueOf(latlng.longitude));
        });

        // se observa el 'livedata' que contiene el estado de la barra de progreso
        sharedViewModel.getProgressBar().observe(getViewLifecycleOwner(), visible -> {
            if (visible)
                binding.loading.setVisibility(ProgressBar.VISIBLE);
            else
                binding.loading.setVisibility(ProgressBar.INVISIBLE);
        });

        // llamada del metodo para iniciar o detener el seguimiento de la ubicacion
        sharedViewModel.switchTrackingLocation();

        // se observa el 'livedata' del usuario autenticado, cuando el usuario cambia se actualiza la variable ''authUser'
        sharedViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            authUser = user;
        });


        // cuando se clica el boton de notificar se crea una incidencia y se guarda en la db de firebase
        binding.buttonNotificar.setOnClickListener(button -> {
            Incidencia incidencia = new Incidencia();
            incidencia.setDireccio(binding.txtDireccio.getText().toString());
            double latitud = Double.parseDouble(binding.txtLatitud.getText().toString().trim());
            double longitud = Double.parseDouble(binding.txtLongitud.getText().toString().trim());
            incidencia.setProblema(binding.txtDescripcio.getText().toString());

            incidencia.setLatitud(latitud);
            incidencia.setLongitud(longitud);

            DatabaseReference base = FirebaseDatabase.getInstance("https://diariopersonal2-d6826-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

            DatabaseReference users = base.child("users");
            DatabaseReference uid = users.child(authUser.getUid());
            DatabaseReference incidencies = uid.child("incidencies");

            DatabaseReference reference = incidencies.push();
            reference.setValue(incidencia);

            // probando marcar mapa
            // se notifica al view model que se ha creado una nueva incidencia
            sharedViewModel.setNewIncidencia(incidencia);
        });

        // se obtienen referencias a boton y la imagen de la foto de la vista
        foto = root.findViewById(R.id.foto);
        Button buttonFoto = root.findViewById(R.id.button_foto);

        buttonFoto.setOnClickListener(button -> {
            // se llama al metodo para abrir la camara y tomar la foto
            dispatchTakePictureIntent();
        });
        return root;
    }

    // metodo para crear un archivo de imagen en el directorio de imagenes
    private File createImageFile() throws IOException {

        // generar el archivo de la imagen con la fecha y hora actuales
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // metodo para abrir la camara y tomar la foto
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // si la camara esta disponible se crea un archivo de imagen y se inicia la actividad de la camara
        if (takePictureIntent.resolveActivity(
                getContext().getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(getContext(),
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    // metodo para cuando la actividad de la camara finaliza
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // si la foto se ha tomado correctamente se muestra en la imagen
        // si no, se muestra el error
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                Glide.with(this).load(photoURI).into(foto);
            } else {
                Toast.makeText(getContext(),
                        "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }



    // para cuando se destruye la vista
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}