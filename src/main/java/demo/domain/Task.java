package demo.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class Task extends TaskOrResource {

    private String id;
    // 任务code
    private String code;
    // 任务产能
    private Integer speed;
    // 单位：0代表层，1代表套
    private Integer unit;
    // 工序顺序
    private String taskOrder;
    // 层
    private Integer layerNum;
    // unit为1且为叠片工序时传入，代表前置层数约束
    private List<Integer> relatedLayer;

    private List<Task> successorTaskList = new ArrayList<>();

    private Product product;

    private TaskType taskType;

//    @PlanningVariable(valueRangeProviderRefs = {"resourceRange", "taskRange"}, graphType = PlanningVariableGraphType.CHAINED)
    private TaskOrResource previousTaskOrResource;

//    @AnchorShadowVariable(sourceVariableName = "previousTaskOrResource")
    private ResourceItem resourceItem;
//    @CustomShadowVariable(variableListenerClass = StartTimeUpdatingVariableListener.class,
//            sources = {@PlanningVariableReference(variableName = "previousTaskOrResource")})
    private Integer startTime;

    private Integer endTime;

    private int readyTime;

//    @PlanningVariable(valueRangeProviderRefs = "scheduleRange")
    private ScheduleDate scheduleDate;


    private String requiredResourceId;

    private Integer requiredResourceIntId;
    //班次
    private Integer schedule;
    //安排好的时间
    private LocalDateTime time;
    //加工的数量
    private Integer amount;

    private String productId;

    private String stepId;

    private Integer stepIndex;

    private ManufacturerOrder manufacturerOrder;

    private Task preTask;

    private Integer duration;

    private BigDecimal timeSlotDuration;

    private BigDecimal singleTimeSlotSpeed;

    private Integer minutesDuration;

    private String orderId;

    private Integer halfHourDuration;

    private Integer hourDuration;

    private LocalDateTime taskBeginTime = LocalDateTime.of(2022, 10, 1, 0, 0, 0);


    public LocalDateTime getActualStartTime(){
        return taskBeginTime.plusMinutes(Optional.ofNullable(this.startTime).orElse(0));
    }
    public LocalDateTime getActualEndTime(){
        return taskBeginTime.plusMinutes(Optional.ofNullable(this.endTime).orElse(0));
    }


    public Task(String id, String code, Integer speed, Integer unit, String taskOrder, Integer layerNum, List<Integer> relatedLayer) {
        this.id = id;
        this.code = code;
        this.speed = speed;
        this.unit = unit;
        this.taskOrder = taskOrder;
        this.layerNum = layerNum;
        this.relatedLayer = relatedLayer;
    }

    public String getFullTaskName() {
        return this.code + " 所在工序组：" + Optional.ofNullable(this.requiredResourceId).orElse("错误：工序组为空")
                + " 开始时间：" + taskBeginTime.plusMinutes(Optional.ofNullable(this.startTime).orElse(0)) +
                " 结束时间：" + taskBeginTime.plusMinutes(Optional.ofNullable(this.endTime).orElse(0));
    }

    @Override
    public Integer getEndTime(int quantity) {
        if (startTime == null) {
            return null;
        }

        this.endTime = startTime + (int) Math.ceil(24.0 * 60 * quantity / this.speed);
        return this.endTime;
    }



}
