package com.github.lindenb.jvarkit.pedigree;

public interface Sample {
	public String getId();
	public Family getFamily();
	public Status getStatus();
	public Sex getSex();
	
	public default boolean isMale() { return Sex.male.equals(this.getSex());}
	public default boolean isFemale() { return Sex.female.equals(this.getSex());}

	public default boolean isAffected() { return Status.affected.equals(this.getStatus());}
	public default boolean isUnaffected() { return Status.unaffected.equals(this.getStatus());}
        
	public boolean hasFather();
	public boolean hasMother();
	public Sample getFather();
	public Sample getMother();
}