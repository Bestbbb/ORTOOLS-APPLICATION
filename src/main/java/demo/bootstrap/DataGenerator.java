package demo.bootstrap;


import com.google.ortools.sat.IntVar;
import demo.domain.*;
import demo.jsonUtils.LoadFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class DataGenerator {
    static String FILE_PATH = "json/input_2.json";
    static Input input;
    public final static String OUTPUT_PATH = "json/output.json";
    public final static String RESULT_PATH = "D:\\result.json";
    static {
        input = LoadFile.readJsonFile(FILE_PATH);
    }
    public static void writeObjectToFile(Object output) {
        LoadFile.writeJsonFile(output, OUTPUT_PATH);
    }

    public static void writeResult(Object output) {
        LoadFile.writeJsonFile(output, RESULT_PATH);
    }

    public static List<ResourceItem> generateResources() {
        List<ResourceItem> resourceItemList = new ArrayList<>();
        List<ResourcePool> resourcePool = input.getResourcePool();
        Integer index = 0;
        resourcePool.forEach(each -> {
            ResourceItem available = each.getAvailableList().get(0);
            available.setResourcePoolId(each.getId());
            System.out.println();
            resourceItemList.add(available);

        });
        return resourceItemList;
    }

    public static List<ManufacturerOrder> generateOrderList() {
        return input.getManufacturerOrderList();
    }



    public static List<Task> generateTaskList() {
        List<ManufacturerOrder> manufacturerOrderList = input.getManufacturerOrderList();
        List<Task> taskList = new ArrayList<>();
//        ManufacturerOrder order = manufacturerOrderList.get(0);
        for (ManufacturerOrder order : manufacturerOrderList) {
            Product product = order.getProduct();
            List<Step> stepList = product.getStepList();
            for(Step step:stepList){
                List<ResourceRequirement> resourceRequirementList = step.getResourceRequirementList();
                List<Task> list = step.getTaskList();
                for(Task task :list) {
                    task.setProduct(product);
                    task.setProductId(product.getId());
                    task.setStepId(step.getId());
                    task.setOrderId(order.getId());
                    //duration 还得修改
                    task.setDuration((int) Math.ceil((double) order.getQuantity() / task.getSpeed()));
                    task.setSingleTimeSlotSpeed(BigDecimal.valueOf(task.getSpeed()).divide(BigDecimal.valueOf(3), 4, RoundingMode.CEILING));
                    task.setTimeSlotDuration(BigDecimal.valueOf(order.getQuantity()).divide(task.getSingleTimeSlotSpeed(), 4, RoundingMode.CEILING));
                    task.setMinutesDuration((int) Math.ceil(24.0 * 60 * order.getQuantity() / task.getSpeed()));
                    task.setHalfHourDuration((int) Math.ceil(48.0 *order.getQuantity()/task.getSpeed()));
                    task.setHourDuration((int)Math.ceil(24.0*order.getQuantity()/task.getSpeed()));
                    task.setManufacturerOrder(order);
                    task.setRequiredResourceId(resourceRequirementList.get(0).getResourceId());
                }
                taskList.addAll(list);
            }
//            for (int i = stepList.size() - 1; i >= 0; i--) {
//                Step step = stepList.get(i);
//                List<ResourceRequirement> resourceRequirementList = step.getResourceRequirementList();
//                List<Task> stepTaskList = step.getTaskList();
//                Collections.reverse(stepTaskList);
//                int number = i;
//                for (int j = stepTaskList.size() - 1; j >= 0; j--) {
//                    Task item = stepTaskList.get(j);
//                    item.setProduct(product);
//                    item.setProductId(product.getId());
//                    item.setStepId(step.getId());
//                    item.setStepIndex(number);
//                    item.setOrderId(order.getId());
//                    //duration 还得修改
//                    item.setDuration((int) Math.ceil((double) order.getQuantity() / item.getSpeed()));
//                    item.setSingleTimeSlotSpeed(BigDecimal.valueOf(item.getSpeed()).divide(BigDecimal.valueOf(3), 4, RoundingMode.CEILING));
//                    item.setTimeSlotDuration(BigDecimal.valueOf(order.getQuantity()).divide(item.getSingleTimeSlotSpeed(), 4, RoundingMode.CEILING));
//                    item.setMinutesDuration((int) Math.ceil(24.0 * 60 * order.getQuantity() / item.getSpeed()));
//                    item.setManufacturerOrder(order);
//                    item.setRequiredResourceId(resourceRequirementList.get(0).getResourceId());
//                    if (number < stepList.size() - 1) {
//                        Task one = taskList.get(taskList.size() - stepTaskList.size() + j);
//                        item.setNextTask(one);
//                        one.setPreTask(item);
//                    }
//                }
//
//
//                taskList.addAll(stepTaskList);
//            }
//            order.setTaskList(taskList);
        }
        //对每个unit = 0的单个任务设置
        Map<String, Map<Integer, List<Task>>> orderIdToLayerNumberToTasks =
                taskList.parallelStream().filter(task -> task.getLayerNum()!=null).collect(Collectors.groupingBy(Task::getOrderId, Collectors.groupingBy(Task::getLayerNum)));
        orderIdToLayerNumberToTasks.forEach(
                (orderId,map)->{
                    map.forEach((layerNumber,tasks)->{
                        //看是否需要对tasks按照id进行排序
                        for(int i = 0;i<tasks.size();i++){
                            if(i!= tasks.size()-1){
                                Task current = tasks.get(i);
                                Task next = tasks.get(i+1);
                                current.setNextTask(next);
                                next.setPreTask(current);
                            }
                        }
                    });
                }
        );
        //对每个unit=1的套型任务的设置
        Map<String,List<Task>> orderIdToTasks =
                taskList.parallelStream().filter(task -> task.getLayerNum()==null).collect(Collectors.groupingBy(Task::getOrderId));
        orderIdToTasks.forEach((orderId,tasks)->{
            for(int i = 0;i<tasks.size();i++){
                if(i!= tasks.size()-1){
                    Task current = tasks.get(i);
                    Task next = tasks.get(i+1);
                    current.setNextTask(next);
                    next.setPreTask(current);
                }
            }
        });
//        连接relatedlayer和套型的task
        for (Task task : taskList) {
            Integer unit = task.getUnit();
            if(unit==1){
                List<Integer> relatedLayer = task.getRelatedLayer();
                if(relatedLayer!=null){
                    String currentTaskId = task.getId();
                    String orderId = task.getOrderId();
                    Map<Integer,List<Task>> layerNumberToTasks= taskList.parallelStream().
                            filter(task1 -> task1.getLayerNum()!=null && task1.getOrderId().equals(orderId) && relatedLayer.contains(task1.getLayerNum()))
                            .collect(Collectors.groupingBy(Task::getLayerNum));
                    List<List<Task>> taskGroups = layerNumberToTasks.values().stream().collect(Collectors.toList());
                    for(int i=0;i<taskGroups.size();i++){
                        if(i!=taskGroups.size()-1){
                            List<Task> preTasks = taskGroups.get(i);
                            Task preTask = preTasks.get(preTasks.size()-1);
                            List<Task> nextTasks = taskGroups.get(i+1);
                            Task nextTask = nextTasks.get(0);
                            preTask.setNextTask(nextTask);
                            nextTask.setPreTask(preTask);

                        }
                        else{
                            List<Task> preTasks = taskGroups.get(i);
                            Task preTask = preTasks.get(preTasks.size()-1);
                            preTask.setNextTask(task);
                            task.setPreTask(preTask);
                        }
                    }


                }

            }
        }
//        System.out.println(taskList.size());
//        Collections.reverse(taskList);
        taskList.forEach(task ->

        {
            if(task.getNextTask()!=null){
//                System.out.println(task.getHalfHourDuration());
                System.out.println(task.getId() +" next: "+ task.getNextTask().getId()+" Hour duration:"+task.getHourDuration()

                +"Resource:"+task.getRequiredResourceId());
            }
        });
        return taskList;
    }

    public static List<Timeslot> generateTimeSlotList() {
        List<Timeslot> timeslotList = new ArrayList<>(3);
//        timeslotList.add(new Timeslot(0, LocalTime.of(0, 0), LocalTime.of(8, 0)));
//        timeslotList.add(new Timeslot(1, LocalTime.of(8, 0), LocalTime.of(16, 0)));
//        timeslotList.add(new Timeslot(2, LocalTime.of(16, 0), LocalTime.of(24, 0)));
        return timeslotList;
    }

    public static void sortTask(List<Task> taskList) {
        for (Task task : taskList) {

            Task successorTask = task.getNextTask();
            while (successorTask != null) {
                task.getSuccessorTaskList().add(successorTask);
                successorTask = successorTask.getNextTask();
            }
        }
//        for (Task task : taskList) {
//            System.out.println(task.getId());
//
//            List<Task> successorTaskList = task.getSuccessorTaskList();
//            for (Task task1 : successorTaskList) {
//                System.out.println("next task"+task1.getId());
//
//            }
//        }
    }

    public static List<Allocation> createAllocationList(List<Task> taskList, List<ManufacturerOrder> orderList) {
        Integer orderListSize = orderList.size();
//        List<Task> taskList = generateTaskList();
        List<Allocation> allocationList = new ArrayList<>(taskList.size());
        Map<String, Allocation> taskToAllocationMap = new HashMap<>(taskList.size());
        Map<ManufacturerOrder, Allocation> orderToSourceAllocationMap = new HashMap<>(orderListSize);
        Map<ManufacturerOrder, Allocation> orderToSinkAllocationMap = new HashMap<>(orderListSize);
        for (Task task : taskList) {
            Allocation allocation = new Allocation();
            allocation.setId(task.getId());
            allocation.setTask(task);
            allocation.setStepIndex(task.getStepIndex());
            allocation.setPredecessorAllocationList(new ArrayList<>(task.getSuccessorTaskList().size()));
            allocation.setSuccessorAllocationList(new ArrayList<>(task.getSuccessorTaskList().size()));
            // Uninitialized allocations take no time, but don't break the predecessorsDoneDate cascade to sink.
            allocation.setPredecessorsDoneDate(task.getManufacturerOrder().getReleaseDate());
            if (task.getTaskType() == TaskType.SOURCE) {
                allocation.setDelay(0);
//                if (job.getExecutionModeList().size() != 1) {
//                    throw new IllegalArgumentException("The job (" + job
//                            + ")'s executionModeList (" + job.getExecutionModeList()
//                            + ") is expected to be a singleton.");
//                }
//                allocation.setExecutionMode(job.getExecutionModeList().get(0));
                orderToSourceAllocationMap.put(task.getManufacturerOrder(), allocation);
            } else if (task.getTaskType() == TaskType.SINK) {
                allocation.setDelay(0);
//                if (job.getExecutionModeList().size() != 1) {
//                    throw new IllegalArgumentException("The job (" + job
//                            + ")'s executionModeList (" + job.getExecutionModeList()
//                            + ") is expected to be a singleton.");
//                }
//                allocation.setExecutionMode(job.getExecutionModeList().get(0));
                orderToSinkAllocationMap.put(task.getManufacturerOrder(), allocation);
            }
            allocationList.add(allocation);
            taskToAllocationMap.put(task.getId(), allocation);
        }
        for (Task task : taskList) {
            if (task.getTaskType().equals(TaskType.SOURCE)) {
                System.out.println(task.getId());
            }
        }
//        System.out.println(taskList.size());
//        System.out.println(allocationList.size());
//        System.out.println(taskToAllocationMap.size());
//
//        System.out.println(taskToAllocationMap.values().size());
        for (Allocation allocation : taskToAllocationMap.values()) {
            System.out.println("task:" + allocation.getTask().getId() + "allocation:" + allocation.getId());
        }
        for (Allocation allocation : allocationList) {
            Task task = allocation.getTask();
            Allocation allocation1 = orderToSourceAllocationMap.get(task.getManufacturerOrder());
//            System.out.println("source:"+allocation1.getId());

            allocation.setSourceAllocation(orderToSourceAllocationMap.get(task.getManufacturerOrder()));
            Allocation allocation2 = orderToSinkAllocationMap.get(task.getManufacturerOrder());
//            System.out.println("sink:"+allocation2.getId());
            allocation.setSinkAllocation(orderToSinkAllocationMap.get(task.getManufacturerOrder()));
            for (Task successorJob : task.getSuccessorTaskList()) {
                System.out.println("currenttask：" + task.getId());
                Allocation successorAllocation = taskToAllocationMap.get(successorJob.getId());
                System.out.println("nextlocation：" + successorAllocation.getId() + "对应的task:" + successorAllocation.getTask().getId());

                allocation.getSuccessorAllocationList().add(successorAllocation);
                successorAllocation.getPredecessorAllocationList().add(allocation);
            }
        }
        for (Allocation sourceAllocation : orderToSourceAllocationMap.values()) {
            for (Allocation allocation : sourceAllocation.getSuccessorAllocationList()) {
                allocation.setPredecessorsDoneDate(sourceAllocation.getEndDate(150));
            }
        }
        return allocationList;
    }

    public static List<ScheduleDate> generateScheduleDateList() {
        List<ScheduleDate> scheduleDateList = new ArrayList<>(14);
        scheduleDateList.add(new ScheduleDate(LocalDateTime.now(), null));

        for (int i = 1; i < 14; i++) {
            scheduleDateList.add(new ScheduleDate(LocalDateTime.now().plusDays(i), null));
        }

        return scheduleDateList;
    }

    public static void main(String[] args) {
//        Integer i = 29;
//        System.out.println(Math.ceil(52.0 / 17));
//        List<ManufacturerOrder> manufacturerOrders = DataGenerator.generateOrderList();
        List<Task> taskList = DataGenerator.generateTaskList();
        System.out.println(taskList.size());
        for (Task task : taskList) {
            System.out.println(task.getId());
        }
//        List<ResourceItem> resourceItems = DataGenerator.generateResources();
//        resourceItems.forEach(i->System.out.println(i.toString()));


    }

    private void reverse(Task task) {

    }

}
