package demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class Timeslot {

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;


    public Timeslot(DayOfWeek dayOfWeek, LocalTime startTime) {
        this(dayOfWeek, startTime, startTime.plusMinutes(50));
    }

    @Override
    public String toString() {
        return dayOfWeek + " " + startTime;
    }


}
