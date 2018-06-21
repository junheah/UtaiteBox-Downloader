package ml.melun.junhea.uboxdownloader;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.view.menu.MenuView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;

public class playlistAdapter extends RecyclerView.Adapter<playlistAdapter.ItemViewHolder>
        implements ItemTouchHelperAdapter{
    private final ArrayList<Item> mData;
    private Context main;
    public playlistAdapter(ArrayList<Item> list, Context context){
        mData = list;
        main = context;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //set layout of item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item, parent, false);
        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }
    @Override

    public void onBindViewHolder(ItemViewHolder holder, int position) {
        //set contents of views
        Item song = mData.get(position);
        holder.songName.setText(song.getName());
        holder.artistName.setText(song.getArtist());
//
        String thumb= song.getThumb();

        //todo: glide inside recycler view doesnt work
        if(thumb.matches("null")) holder.thumbView.setImageResource(R.drawable.default_cover);
        else{
            thumb = "http://utaitebox.com/res/artist/image/" + thumb;
            //System.out.println(thumb);
            Glide.with(main.getApplicationContext())
                    .load(thumb)
                    .placeholder(R.drawable.default_cover)
                    .into(holder.thumbView);
        }
        //holder.thumbView.setImageResource(R.drawable.default_artist);
    }
//
//    public Item getItem(int position) {
//        return mData.get(position);
//    }
//
//    public void addItem(final Item item) {
//        mData.add(item);
//    }

    @Override
    public int getItemCount() {
        return mData.size();
    }
    @Override
    public boolean onItemDismiss(final int position) {
        mData.remove(position);
        notifyItemRemoved(position);
        return true;
    }
    @Override

    public boolean onItemMove(final int fromPosition, final int toPosition) {
        //System.out.println(fromPosition +" to " + toPosition);
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mData, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mData, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }


    public static class ItemViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder {
        public TextView songName;
        public TextView artistName;
        public ImageView thumbView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            songName = itemView.findViewById(R.id.plistSong);
            artistName = itemView.findViewById(R.id.plistArtist);
            thumbView = itemView.findViewById(R.id.plistThumb);
        }

        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }
}
