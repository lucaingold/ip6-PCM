package ch.fhnw.ip5.powerconsumptionmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.fhnw.ip5.powerconsumptionmanager.R;
import ch.fhnw.ip5.powerconsumptionmanager.util.ChartHelper;
import ch.fhnw.ip5.powerconsumptionmanager.util.PowerConsumptionManagerAppContext;

/**
 * Created by Patrik on 03.12.2015.
 */
public class DeviceListAdapter extends ArrayAdapter<String> {
    private int mLayout;
    private List<String> mDevices;
    private HashMap<Integer, LineDataSet> mConsumptionDataSet = new HashMap<Integer, LineDataSet>();
    private ChartHelper mConsumptionChart;
    private ArrayList<Integer> removedDataSetIndexes = new ArrayList<>();

    public DeviceListAdapter(Context context, int resource, ArrayList<String> objects, ChartHelper chart) {
        super(context, resource, objects);
        mLayout = resource;
        mDevices = objects;
        mConsumptionChart = chart;
        mConsumptionDataSet = chart.getConsumptionDataSet();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder mainViewHolder = null;

        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(mLayout, parent, false);
            final ViewHolder vh = new ViewHolder();
            vh.textDevice = (TextView) convertView.findViewById(R.id.textDevice);
            vh.textDevice.setText(mDevices.get(position));
            vh.switchDevice = (Switch) convertView.findViewById(R.id.switchDevice);
            vh.switchDevice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int shiftPos = getShiftPosition(position);
                    if(!vh.switchDevice.isChecked()) {
                        mConsumptionDataSet.remove(position-shiftPos);
                        mConsumptionChart.getChart().getLineData().removeDataSet(position-shiftPos);
                        mConsumptionChart.displayNoneAnimated();

                        removedDataSetIndexes.add(position);
                    }
                    else {
                        PowerConsumptionManagerAppContext appContext = (PowerConsumptionManagerAppContext) getContext().getApplicationContext();
                        boolean skip = false;
                        /*
                        for(int i = 0; i < appContext.getConsumptionData().size(); i++) {
                            for(int j = 0; j < removedDataSetIndexes.size(); j++) {
                                if(removedDataSetIndexes.get(j) == i) {
                                    skip = true;
                                    break;
                                }
                            }
                            if(skip) {
                                skip = false;
                                continue;
                            }
                            mConsumptionChart.generateDataSet(appContext.getConsumptionData().get(i), i);
                        }
                        */

                        mConsumptionChart.generateDataSet(appContext.getConsumptionData().get(position-shiftPos), position-shiftPos);
                        mConsumptionChart.setChartData();
                        mConsumptionChart.displayNoneAnimated();
                        removedDataSetIndexes.remove(Integer.valueOf(position));
                    }
                }
            });
            convertView.setTag(vh);
        }
        else {
            mainViewHolder = (ViewHolder) convertView.getTag();
        }

        return convertView;
    }

    public int getShiftPosition(int position) {
        int shiftPos = 0;
        for(int i = 0; i < removedDataSetIndexes.size(); i++) {
            if(removedDataSetIndexes.get(i) < position) {
                shiftPos++;
            }
        }
        return shiftPos;
    }

    static class ViewHolder {
        TextView textDevice;
        Switch switchDevice;
    }
}
