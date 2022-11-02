package demo.domain;

import java.util.Comparator;

public class SortTasks implements Comparator<AssignedTask> {

    @Override
    public int compare(AssignedTask o1, AssignedTask o2) {
        if(o1.getSchedule()!=o2.getSchedule()){
            return o1.getSchedule()-o2.getSchedule();
        }else{
            return o1.getSchedule()-o2.getSchedule();
        }

    }
}
