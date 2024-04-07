package com.example.ttv;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ttv.chatbox.ObjectData;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ObjectListAdapter extends RecyclerView.Adapter<ObjectListAdapter.ViewHolder> {
    private List<ObjectData> objectList;
    private OnItemClickListener itemClickListener;
    private OnEditClickListener editClickListener;
    private OnNameClickListener nameClickListener;


    public interface OnNameClickListener { // Add this interface
        void onNameClick(ObjectData objectData);
    }

    public interface OnItemClickListener {
        void onItemClick(ObjectData objectData);
    }

    public interface OnEditClickListener {
        void onEditClick(ObjectData objectData);
    }

    public interface OnImageClickListener {
        void onImageClick(Bitmap objImage);
    }

    private OnImageClickListener imageClickListener;

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.imageClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.editClickListener = listener;
    }

    public void setNameClickListener(OnNameClickListener listener) {
        this.nameClickListener = listener;
    }

    public ObjectListAdapter(List<ObjectData> objectList) {
        this.objectList = objectList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.data_object, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ObjectData objectData = objectList.get(position);
        holder.objNameTextView.setText(objectData.getObjName());
        holder.objImageView.setImageBitmap(objectData.getObjImage());
        String imageUrl = objectData.getObjImageURL();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.with(holder.itemView.getContext()).load(imageUrl).into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return objectList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        TextView objNameTextView;
        ImageView objImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            itemView.setOnClickListener(v -> {
//                if (itemClickListener != null) {
//                    itemClickListener.onItemClick(objectList.get(getAdapterPosition()));
//                }
//            });

            // Update the IDs to match those in data_object.xml
            objNameTextView = itemView.findViewById(R.id.obj_name_text_view);
            objImageView = itemView.findViewById(R.id.obj_image_view);
            // Set click listener for objNameTextView
            objNameTextView.setOnClickListener(v -> {
                if (nameClickListener != null) {
                    nameClickListener.onNameClick(objectList.get(getAdapterPosition()));
                }
            });

            // Set click listener for objImageView
            objImageView.setOnClickListener(v -> {
                if (imageClickListener != null) {
                    imageClickListener.onImageClick(objectList.get(getAdapterPosition()).getObjImage());
                }
            });
        }
    }


}
