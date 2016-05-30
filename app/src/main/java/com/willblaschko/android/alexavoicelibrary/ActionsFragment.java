package com.willblaschko.android.alexavoicelibrary;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.willblaschko.android.alexavoicelibrary.actions.SendTextActionFragment;
import com.willblaschko.android.alexavoicelibrary.actions.adapter.ActionFragmentAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by will on 5/30/2016.
 */

public class ActionsFragment extends Fragment {

    private RecyclerView recycler;
    private ActionFragmentAdapter adapter;

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.app_name);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_actions, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = (RecyclerView) view.findViewById(R.id.recycler);
        adapter = new ActionFragmentAdapter(getItems());
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    private List<ActionFragmentAdapter.ActionFragmentItem> getItems(){
        List<ActionFragmentAdapter.ActionFragmentItem> items = new ArrayList<>();
        items.add(new ActionFragmentAdapter.ActionFragmentItem("Send Text",
                android.R.drawable.ic_menu_edit,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadFragment(new SendTextActionFragment());
                    }
                }));

        return items;
    }

    private void loadFragment(Fragment fragment){
        if(getActivity() != null && getActivity() instanceof ActionFragmentInterface){
            ((ActionFragmentInterface) getActivity()).loadFragment(fragment, true);
        }
    }

    public interface ActionFragmentInterface{
        void loadFragment(Fragment fragment, boolean addToBackstack);
    }
}
