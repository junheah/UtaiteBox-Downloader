package ml.melun.junhea.uboxdownloader.Adapter;
//todo 플레이리스트 변경시 서비스 플레이리스트도 변경
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import ml.melun.junhea.uboxdownloader.Item;
import ml.melun.junhea.uboxdownloader.ItemTouchHelper.ItemTouchHelperAdapter;
import ml.melun.junhea.uboxdownloader.ItemTouchHelper.ItemTouchHelperViewHolder;
import ml.melun.junhea.uboxdownloader.MainActivity;
import ml.melun.junhea.uboxdownloader.PlayerService;
import ml.melun.junhea.uboxdownloader.R;

import static ml.melun.junhea.uboxdownloader.PlayerService.ACTION_PLAYLIST;

public class playlistAdapter extends RecyclerView.Adapter<playlistAdapter.ItemViewHolder>
        implements ItemTouchHelperAdapter {
    private ArrayList<Item> mData;
    private Context main;
    private int nowPlaying = -1;
    private Intent plUpdater;

    public playlistAdapter(ArrayList<Item> list, Context context){
        mData = list;
        main = context;
        nowPlaying = -1;
        plUpdater = new Intent(main.getApplicationContext(),PlayerService.class);
        plUpdater.setAction(ACTION_PLAYLIST);
    }
    public String getPlayList(){
        JSONArray playlist = new JSONArray();
        for(Item i : mData){
            try{
                playlist.put(new JSONObject(i.getJSON()));
            }catch(Exception e){
                //
            }
        }
        return(playlist.toString());
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //set layout of item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.playlist_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
        //set contents of views
        Item song = mData.get(position);
        holder.songName.setText(song.getName());
        holder.artistName.setText(song.getArtist());
        if(position==nowPlaying){
            holder.cv.setCardBackgroundColor(Color.LTGRAY);
        }else{
            holder.cv.setCardBackgroundColor(Color.WHITE);
        }
//
        String thumb= song.getThumb();

        //todo: glide inside recycler view doesnt work
        if(thumb.matches("null")) holder.thumbView.setImageResource(R.drawable.default_cover);
        else{
            thumb = "http://utaitebox.com/res/cover/" + thumb;
            Glide.with(main)
                    .load(thumb)
                    .placeholder(R.drawable.default_cover)
                    .dontAnimate()
                    .into(holder.thumbView);
        }
        //holder.thumbView.setImageResource(R.drawable.default_artist);
    }

    public Item getItem(int position) {
        return mData.get(position);
    }



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

    public void swap(ArrayList<Item> data)
    {
        if(data == null || data.size()==0) return;
        if (mData != null && mData.size()>0) mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
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
        System.out.println(toPosition + "      " + fromPosition);
        if(fromPosition == nowPlaying) nowPlaying = toPosition;
        else if(toPosition == nowPlaying) nowPlaying = fromPosition;
        notifyPlaylistChange();
        return true;
    }

    public void notifyPlaylistChange(){
        //submit change to service
        plUpdater.putExtra("playlist",getPlayList());
        plUpdater.putExtra("position",nowPlaying);
        if (Build.VERSION.SDK_INT >= 26) {
            main.startForegroundService(plUpdater);
        }else{
            main.startService(plUpdater);
        }
        //todo submit change to server
    }


    public void setPosition(int pos){
        if(pos!=nowPlaying) {
            int prev = nowPlaying;
            nowPlaying = pos;
            notifyItemChanged(prev);
            notifyItemChanged(nowPlaying);
        }
    }


    public static class ItemViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder{
        public TextView songName;
        public TextView artistName;
        public ImageView thumbView;
        public CardView cv;
        public ItemViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.card_view);
            songName = itemView.findViewById(R.id.plistSong);
            artistName = itemView.findViewById(R.id.plistArtist);
            thumbView = itemView.findViewById(R.id.plistThumb);
        }
        @Override
        public void onItemSelected() {
            //
        }
        @Override
        public void onItemClear() {
            //
        }

    }

}
