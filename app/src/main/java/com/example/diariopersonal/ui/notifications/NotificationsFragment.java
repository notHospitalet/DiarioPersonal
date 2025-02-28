package com.example.diariopersonal.ui.notifications;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;


import com.example.diariopersonal.R;
import com.example.diariopersonal.databinding.FragmentNotificationsBinding;
import com.example.diariopersonal.ui.Incidencia;
import com.example.diariopersonal.ui.home.HomeViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Objects;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private FirebaseAuth auth; // maneja la autenticacion con firebase
    private DatabaseReference incidencias; // referencia a la bd de firebase para acceder a las incincidencias


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // se infla el layout
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // se carga la configuracion de openStreetMap
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // se establece el proveedor de mapas
        binding.map.setTileSource(TileSourceFactory.MAPNIK);

        // controles para mover y hacer zoom del mapa
        binding.map.setMultiTouchControls(true);
        IMapController mapController = binding.map.getController();
        mapController.setZoom(15);

        // todo: apuntes de las coordenadas que quiero meter en el mapa
        // alhambra granada: 37.1717, -3.5881
        // mezquita de cordoba: 37.878865429277, -4.7793416346139
        // inicio: nules: 39.85325, -0.155

        // marcador inicial en nules
        GeoPoint Nules = new GeoPoint(39.85325, -0.155);
        mapController.setCenter(Nules);

        // marcador en nules con icono personalizado y se añade al mapa
        Marker startMarker = new Marker(binding.map);
        startMarker.setPosition(Nules);
        startMarker.setTitle("Nules");
        startMarker.setIcon(requireContext().getDrawable(R.drawable.ic_home_black_24dp));
        binding.map.getOverlays().add(startMarker);

        // capa para mostrar la ubicacion actual del usuario
        MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), binding.map);
        myLocationOverlay.enableMyLocation();
        binding.map.getOverlays().add(myLocationOverlay);

        // se añade una brujula al mapa
        CompassOverlay compassOverlay = new CompassOverlay(requireContext(), binding.map);
        compassOverlay.enableCompass();
        binding.map.getOverlays().add(compassOverlay);

        // se obtiene instancia de firebaseAuth para acceder al usuario autenticado
        auth = FirebaseAuth.getInstance();
        DatabaseReference base = FirebaseDatabase.getInstance("https://diariopersonal2-d6826-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        DatabaseReference users = base.child("users");
        DatabaseReference uid = users.child(auth.getUid());
        incidencias = uid.child("incidencies"); // ruta donde se almacenan las incidencias

        Log.d("LOG: REF. DB NTF", incidencias.toString());

        // se añade un listener a firebase que se ejecuta cada vez que se añade una incidencia
        incidencias.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (binding == null || binding.map == null) {
                    Log.e("FirebaseError", "El fragmento ya no está visible.");
                    return;
                }

                // se obtiene la incidencia y las coordenadas
                Incidencia incidencia = snapshot.getValue(Incidencia.class);
                Double latitud = snapshot.child("latitud").getValue(Double.class);
                Double longitud = snapshot.child("longitud").getValue(Double.class);
                Log.d("LOG: addChildEventListener", latitud + " " + longitud);

                // probando marcar mapa
                // si la incidencia es vlaida se crea un marcador en su ubicacion y se añade al mapa
                if (incidencia != null) {
                    GeoPoint location = new GeoPoint(latitud, longitud);

                    Marker marker = new Marker(binding.map);
                    marker.setPosition(location);
                    marker.setTitle(incidencia.getProblema());
                    marker.setSnippet(incidencia.getDireccio());

                    binding.map.getOverlays().add(marker);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // probando marcar mapa
        // instancia del homeViewModel
        HomeViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // se observa el livedata de nuevas incidencias y se actualiza el mapa si hay una nueva
        sharedViewModel.getNewIncidencia().observe(getViewLifecycleOwner(), incidencia -> {
            if (incidencia != null && binding != null) {
                agregarMarcador(incidencia);
            }
        });
        return root;
    }
    // probando marcar mapa
    // metodo para agregar un marcador de forma manual, se llama cuando hay una nueva incidencia
    private void agregarMarcador(Incidencia incidencia) {
        GeoPoint location = new GeoPoint(incidencia.getLatitud(), incidencia.getLongitud());

        Marker marker = new Marker(binding.map);
        marker.setPosition(location);
        marker.setTitle(incidencia.getProblema());
        marker.setSnippet(incidencia.getDireccio());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        binding.map.getOverlays().add(marker);
        binding.map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.map.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}