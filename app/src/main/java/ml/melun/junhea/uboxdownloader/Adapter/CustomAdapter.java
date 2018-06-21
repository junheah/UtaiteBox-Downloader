package ml.melun.junhea.uboxdownloader.Adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.TreeSet;

import ml.melun.junhea.uboxdownloader.Item;
import ml.melun.junhea.uboxdownloader.R;

/**
 * Created by P.Thinesh on 30/11/2016.
 * sourcd : http://learnlinky.com/2016/11/listview-section-header-android/
 */

public class CustomAdapter extends BaseAdapter {

    private ArrayList<Item> mData = new ArrayList<>();
    private LayoutInflater mInflater;
    Context main;
    public CustomAdapter(Context context) {
        main = context;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addItem(final Item item) {
        mData.add(item);
    }

    public void addSectionHeaderItem(final Item item) {
        mData.add(item);
    }
    @Override
    public int getItemViewType(int position) {
        return mData.get(position).getType();
    }


    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Item getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Item song = mData.get(position);
        int Type = getItemViewType(position);
        if (convertView == null) {
            holder = new ViewHolder();
            switch (Type) {
                //todo: 타입 별로 각각 레이아웃 만들기
                case -1:
                    convertView = mInflater.inflate(R.layout.list_header, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.textSeparator);
                    break;
                case 0:
                    convertView = mInflater.inflate(R.layout.list_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.txtName);
                    holder.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
                    holder.txtImage = convertView.findViewById(R.id.txtImage);
                    break;
                case 1:
                    convertView = mInflater.inflate(R.layout.list_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.txtName);
                    holder.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
                    holder.txtImage = convertView.findViewById(R.id.txtImage);
                    break;
                case 2:
                    convertView = mInflater.inflate(R.layout.list_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.txtName);
                    holder.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
                    holder.txtImage = convertView.findViewById(R.id.txtImage);
                    break;
                case 3:
                    convertView = mInflater.inflate(R.layout.list_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.txtName);
                    holder.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
                    holder.txtImage = convertView.findViewById(R.id.txtImage);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if(Type == -1){
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(song.getName());
        }else if(Type==0){
            holder.txtValue.setVisibility(View.GONE);
            holder.txtImage.setVisibility(View.GONE);
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(song.getName());
        }else if(Type==1){
            holder.txtImage.setVisibility(View.VISIBLE);
            holder.txtValue.setVisibility(View.GONE);
            holder.textView.setVisibility(View.VISIBLE);
            holder.textView.setText(song.getArtist());
            String thumb= song.getThumb();
            if(thumb.matches("null")) holder.txtImage.setImageResource(R.drawable.default_artist);
            else{
                thumb = "http://utaitebox.com/res/artist/image/" + thumb;
                Glide.with(main).load(thumb).into(holder.txtImage);
            }

        }
        else if(Type==2 || Type==3){
            holder.txtImage.setVisibility(View.VISIBLE);
            holder.txtValue.setVisibility(View.VISIBLE);
            holder.textView.setVisibility(View.VISIBLE);
            String thumb = song.getThumb();
            if(thumb.matches("null")) holder.txtImage.setImageResource(R.drawable.default_cover);
            else {
                thumb = "http://utaitebox.com/res/cover/" + thumb;
                Glide.with(main)
                        .load(thumb)
                        .into(holder.txtImage);
            }
            holder.textView.setText(song.getName());
            holder.txtValue.setText(song.getArtist());

        }
        return convertView;
    }

    public static class ViewHolder {
        public TextView textView;
        public TextView txtValue;
        public ImageView txtImage;
    }
}
