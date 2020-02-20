package annotations;

import java.text.DecimalFormat;

import organisation.goal.GoalNode;

public class DataLoad extends Annotation {
	
	protected GoalNode sender;
	protected double value;

	public GoalNode getSender() {
		return sender;
	}

	public void setSender(GoalNode sender) {
		this.sender = sender;
	}

	public DataLoad(String id, GoalNode sender, double amount) {
        super(id);
        this.value = amount;
		this.sender = sender;
	}
	
	public Object getValue() {
		return (double) value;
	}
	
	public void setValue(Object amount) {
		this.value = (double) amount;
	}

	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.##");
		return this.getClass().getSimpleName().substring(0, 1) + "[" + this.id + ":" + this.sender.getGoalName()
				+ ":" + df.format(value) + "]";
	}
	
	public DataLoad clone() {
		DataLoad clone = new DataLoad(this.id, sender, (double) this.value);
	
	    return clone;
	}
}
