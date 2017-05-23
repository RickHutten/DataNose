package nl.workmoose.datanose.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Class that adds the possibility to add a OnScrollChangedListener
 */
public class OnScrollChangedView extends ScrollView {

    private OnScrollChangedListener mOnScrollChangedListener;

    public OnScrollChangedView(Context context) {
        super(context);
    }

    public OnScrollChangedView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        mOnScrollChangedListener = listener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(this, l, t, oldl, oldt);
        }
    }

    public interface OnScrollChangedListener {
        void onScrollChanged(ScrollView view, int x, int y, int oldx, int oldy);
    }
}
