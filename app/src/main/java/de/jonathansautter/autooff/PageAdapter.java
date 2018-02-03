package de.jonathansautter.autooff;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

class PageAdapter extends FragmentPagerAdapter {

    private SparseArray<Fragment> registeredFragments = new SparseArray<>();
    private final String[] TITLES;

    PageAdapter(FragmentManager fm, Context ct) {
        super(fm);
        TITLES =  new String[]{ct.getString(R.string.countdown), ct.getString(R.string.time), ct.getString(R.string.boot), ct.getString(R.string.inactivity)};
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TITLES[position];
    }

    @Override
    public int getCount() {
        return TITLES.length;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            default:
                return null;
            case 0:
                return new Minute_Fragment();
            case 1:
                return new Time_Fragment();
            case 2:
                return new Boot_Fragment();
            case 3:
                return new Inactivity_Fragment();
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }

    /*@Override
    public View getCustomTabView(ViewGroup parent, int position) {
        LinearLayout imageView = (LinearLayout) LayoutInflater.from(mContext)
                .inflate(R.layout.tab_layout, null, false);

        ImageView tabImage = (ImageView) imageView.findViewById(R.id.tabImage);
        tabImage.setImageResource(ICONS[position]);
        tabImage.setAlpha(0.5f);

        return imageView;
    }*/

    /*@Override
    public void tabSelected(View tab) {
        ImageView tabImage = (ImageView) tab.findViewById(R.id.tabImage);
        tabImage.setAlpha(1.0f);
    }

    @Override
    public void tabUnselected(View tab) {
        ImageView tabImage = (ImageView) tab.findViewById(R.id.tabImage);
        tabImage.setAlpha(0.5f);
    }*/
}