package demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignedTask implements Serializable {

    private String originalId;
    private Integer subId;
    private String code;
    private Integer speed;
    private Integer unit;
    private Integer layerNum;
    private Integer taskType;
    private List<Integer> relatedLayer;
    private Integer amount;
    private LocalDateTime runTime;
    private Integer schedule;

    private String StepId;

    private Integer start;

    private Integer duration;


    public AssignedTask(String originalId,Integer start,Integer duration){
        this.originalId =originalId;
        this.start=start;
        this.duration=duration;
    }

    public String printInfo() {
        return "日期:" + runTime + " 班次:" + schedule + " 工序组:" + StepId + " 生产总数:" + amount + " speed:" + speed;
    }

}
