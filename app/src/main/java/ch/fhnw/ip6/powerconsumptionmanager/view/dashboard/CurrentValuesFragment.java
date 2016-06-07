package ch.fhnw.ip6.powerconsumptionmanager.view.dashboard;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.gigamole.library.ArcProgressStackView;

import ch.fhnw.ip6.powerconsumptionmanager.R;
import ch.fhnw.ip6.powerconsumptionmanager.util.helper.DashboardHelper;

public class CurrentValuesFragment extends Fragment {
    private DashboardHelper mDashBoardHelper;

    public static CurrentValuesFragment newInstance() {
        return new CurrentValuesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_now, container, false);

        mDashBoardHelper = DashboardHelper.getInstance();
        mDashBoardHelper.initCurrentValuesContext(getContext());
        mDashBoardHelper.setDynamicLayoutContainer((LinearLayout) view.findViewById(R.id.dynamic_components_container));

        if(mDashBoardHelper.getDynamicLayoutContainerWidth() == 0 && mDashBoardHelper.getDynamicLayoutContainerHeight() == 0) {
            setupViewTreeObserver(view);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDashBoardHelper.getArcsvIdsMap().clear();

        mDashBoardHelper.generateComponentUIElement("Boiler", ContextCompat.getColor(getContext(), R.color.colorDynamicConsumer1));
        mDashBoardHelper.generateComponentUIElement("Wärmepumpe", ContextCompat.getColor(getContext(), R.color.colorDynamicConsumer2));
        mDashBoardHelper.generateComponentUIElement("Emobil", ContextCompat.getColor(getContext(), R.color.colorDynamicConsumer3));

        mDashBoardHelper.displayAnimated();
    }

    private void setupViewTreeObserver(View view) {
        final LinearLayout dynamicContentContainer = (LinearLayout) view.findViewById(R.id.dynamic_components_container);
        ViewTreeObserver vto = dynamicContentContainer.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < 16) {
                    dynamicContentContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    dynamicContentContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                mDashBoardHelper.setDynamicLayoutContainerWidth(dynamicContentContainer.getWidth());
                mDashBoardHelper.setDynamicLayoutContainerHeight(dynamicContentContainer.getWidth());

                RelativeLayout.LayoutParams arcsvLayoutParams = new RelativeLayout.LayoutParams(
                        dynamicContentContainer.getWidth(),
                        dynamicContentContainer.getHeight()
                );
                arcsvLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                int margins = (int) mDashBoardHelper.getDensity() * 8;
                arcsvLayoutParams.setMargins(margins, margins, margins, margins);

                for (int id : mDashBoardHelper.getArcsvIdsMap().values()) {
                    ArcProgressStackView arcsv = (ArcProgressStackView) getView().findViewById(id);
                    arcsv.setLayoutParams(arcsvLayoutParams);
                }
            }
        });
    }
}
