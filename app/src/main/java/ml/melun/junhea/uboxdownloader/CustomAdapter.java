package ml.melun.junhea.uboxdownloader;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Created by P.Thinesh on 30/11/2016.
 * sourcd : http://learnlinky.com/2016/11/listview-section-header-android/
 */

public class CustomAdapter extends BaseAdapter {

    private ArrayList<Item> mData = new ArrayList<>();
    private LayoutInflater mInflater;

    public CustomAdapter(Context context) {
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
                    break;
                case 1:
                    convertView = mInflater.inflate(R.layout.list_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.txtName);
                    holder.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
                    break;
                case 2:
                    convertView = mInflater.inflate(R.layout.list_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.txtName);
                    holder.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
                    break;
                case 3:
                    convertView = mInflater.inflate(R.layout.list_item, null);
                    holder.textView = (TextView) convertView.findViewById(R.id.txtName);
                    holder.txtValue = (TextView) convertView.findViewById(R.id.txtValue);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if(Type == -1){
            holder.textView.setText(mData.get(position).getName());
        }else if(Type==0 || Type==1){
            holder.textView.setText(mData.get(position).getName());
            holder.txtValue.setText(""+mData.get(position).getId());
        }else if(Type==2 || Type==3){
            holder.textView.setText(mData.get(position).getName());
            holder.txtValue.setText(mData.get(position).getKey());
        }
        return convertView;
    }

    public static class ViewHolder {
        public TextView textView;
        public TextView txtValue;
    }
}
