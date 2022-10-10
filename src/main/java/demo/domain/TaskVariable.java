package demo.domain;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;
import lombok.Data;

@Data
public class TaskVariable {
    private IntVar start;
    private IntVar end;
    private IntervalVar interval;

}
