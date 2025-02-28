package com.example.diariopersonal.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diariopersonal.databinding.FragmentDashboardBinding;
import com.example.diariopersonal.databinding.RvIncidenciesBinding;
import com.example.diariopersonal.ui.Incidencia;
import com.example.diariopersonal.ui.home.HomeViewModel;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseUser authUser;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        DashboardViewModel dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        HomeViewModel sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // se infla el layout
        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        // se observa el 'livedata' del usuario autenticado
        sharedViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            authUser = user;

            // si el usuario esta autenticado se obtiene una referencia de la db de firebase
            // se accede a la ruta donde 'uid' es el usuario autenticado,se almacenan las incidencias
            if (user != null) {
                DatabaseReference base = FirebaseDatabase.getInstance("https://diariopersonal2-d6826-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
                DatabaseReference users = base.child("users");
                DatabaseReference uid = users.child(authUser.getUid());
                DatabaseReference incidencies = uid.child("incidencies");

                // objeto que define como se deben obtener los datos de firebase
                FirebaseRecyclerOptions<Incidencia> options = new FirebaseRecyclerOptions.Builder<Incidencia>()
                        .setQuery(incidencies, Incidencia.class)
                        .setLifecycleOwner(this)
                        .build();

                // se crea el adaptador para manejar la conexion entre los datos de firebase y el recyclerview
                // y se configura el recycler view
                IncidenciaAdapter adapter = new IncidenciaAdapter(options);
                binding.rvIncidencies.setAdapter(adapter);
                binding.rvIncidencies.setLayoutManager(
                        new LinearLayoutManager(requireContext())
                );

            }
        });

        // se obtiene y devuelve la vista
        View root = binding.getRoot();
        return root;
    }

    // metodo para cuando se destruye la vista
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // adaptador para manejar la conexion entre los datos de firebase y el recyclerview
    class IncidenciaAdapter extends FirebaseRecyclerAdapter<Incidencia, IncidenciaAdapter.IncidenciaViewholder> {
        public IncidenciaAdapter(@NonNull FirebaseRecyclerOptions<Incidencia> options) {
            super(options);
        }

        // metodo para enlazar los datos de una incidencia ocn las vistas del viewholder
        @Override
        protected void onBindViewHolder(
                @NonNull IncidenciaViewholder holder, int position, @NonNull Incidencia model
        ) {
            // se obtienen los datos y se asignan a los textview
            holder.binding.txtDescripcio.setText(model.getProblema());
            holder.binding.txtAdreca.setText(model.getDireccio());
        }

        // metodo para crear un viewholder cuando el recyclerview lo necesita
        @NonNull
        @Override
        public IncidenciaViewholder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType
        ) {
            return new IncidenciaViewholder(RvIncidenciesBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent, false));
        }

        // clase para acceder a las vistas del layout rv_incidencias
        class IncidenciaViewholder extends RecyclerView.ViewHolder {
            RvIncidenciesBinding binding;

            public IncidenciaViewholder(RvIncidenciesBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}