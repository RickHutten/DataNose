package nl.workmoose.datanose;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {

    public interface OnScrollChangedListener {
        void onScrollChanged(ScrollView view, int x, int y, int oldx, int oldy);
    }

    private OnScrollChangedListener mOnScrollChangedListener;

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

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
