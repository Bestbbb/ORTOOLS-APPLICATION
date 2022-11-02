package demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssignedTask {
    private String taskId;

    private Integer start;

    private Integer duration;
    //班次
    private Integer schedule;
    //数量
    private Integer amount;

}
