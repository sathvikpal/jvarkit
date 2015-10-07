/*
The MIT License (MIT)

Copyright (c) 2014 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


History:
* 2014 creation

*/
package com.github.lindenb.jvarkit.tools.samfixcigar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.lindenb.jvarkit.util.command.Command;
import com.github.lindenb.jvarkit.util.picard.SAMSequenceDictionaryProgress;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.util.CloserUtil;

import com.github.lindenb.jvarkit.util.picard.GenomicSequence;

public class SamFixCigar extends AbstractSamFixCigar
	{
	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SamFixCigar.class);

	@Override
	public Command createCommand() {
		return new MyCommand();
		}
	
	static private class MyCommand extends AbstractSamFixCigar.AbstractSamFixCigarCommand
		{    
		private IndexedFastaSequenceFile indexedFastaSequenceFile=null;
		
		@Override
		protected Collection<Throwable> call(String inputName) throws Exception {
				GenomicSequence genomicSequence=null;			
				if(faidx==null)
					{
					return wrapException("Reference was not specified.");
					}
				SamReader sfr=null;
				SAMFileWriter sfw=null;
				try
					{
					LOG.info("Loading reference");
					this.indexedFastaSequenceFile=new IndexedFastaSequenceFile(faidx);
					sfr =  openSamReader(inputName);
					SAMFileHeader header=sfr.getFileHeader();
					
					sfw = openSAMFileWriter(header,true);
					SAMSequenceDictionaryProgress progress= new SAMSequenceDictionaryProgress(header);
					List<CigarElement> newCigar=new ArrayList<CigarElement>();
					SAMRecordIterator iter=sfr.iterator();
					while(iter.hasNext())
						{
						SAMRecord rec=progress.watch(iter.next());
						Cigar cigar=rec.getCigar();
						byte bases[]=rec.getReadBases();
						if( rec.getReadUnmappedFlag() ||
							cigar==null ||
							cigar.getCigarElements().isEmpty() ||
							bases==null)
							{
							sfw.addAlignment(rec);
							continue;
							}
						
						if(genomicSequence==null ||
							genomicSequence.getSAMSequenceRecord().getSequenceIndex()!=rec.getReferenceIndex())
							{
							genomicSequence=new GenomicSequence(indexedFastaSequenceFile, rec.getReferenceName());
							}
						
						newCigar.clear();
						int refPos1=rec.getAlignmentStart();
						int readPos0=0;
						
						for(CigarElement ce:cigar.getCigarElements())
							{
							switch(ce.getOperator())
								{
		    					case H:break;
		
		    					
		    					case P:break;
								case N://cont
								case D:
									{
									newCigar.add(ce);
									refPos1+=ce.getLength();
									break;
									}
								case S:
								case I:
									{
									newCigar.add(ce);
									readPos0+=ce.getLength();							
									break;
									}
								case EQ://cont
								case X:
									{
									newCigar.add(ce);
									refPos1+=ce.getLength();
									readPos0+=ce.getLength();	
									break;
									}
								case M:
									{
									for(int i=0;i< ce.getLength();++i)
			    		    			{
										char c1=Character.toUpperCase((char)bases[readPos0]);
										char c2=Character.toUpperCase(refPos1-1< genomicSequence.length()?genomicSequence.charAt(refPos1-1):'*');
										
										if(c2=='N' || c1==c2)
											{
											newCigar.add(new CigarElement(1, CigarOperator.EQ));
											}
										else
											{
											newCigar.add(new CigarElement(1, CigarOperator.X));
											}
										
			    						refPos1++;
			    						readPos0++;
		    		    				}
									break;
									}
								default: throw new RuntimeException("Cannot parse cigar "+rec.getCigarString()+" in "+rec.getReadName());
								}
							}
						int i=0;
						while(i< newCigar.size())
							{
							CigarOperator op1 = newCigar.get(i).getOperator();
							int length1 = newCigar.get(i).getLength();
							
							if( i+1 <  newCigar.size() &&
								newCigar.get(i+1).getOperator()==op1)
								{
								CigarOperator op2= newCigar.get(i+1).getOperator();
								int length2=newCigar.get(i+1).getLength();
		
								 newCigar.set(i,new CigarElement(length1+length2, op2));
								 newCigar.remove(i+1);
								}
							else
								{
								++i;
								}
							}
						cigar=new Cigar(newCigar);
						rec.setCigar(cigar);
						
						sfw.addAlignment(rec);
						}
					progress.finish();
					return RETURN_OK;
					}
				catch(Exception err)
					{
					return wrapException(err);
					}
				finally
					{
					CloserUtil.close(this.indexedFastaSequenceFile);
					this.indexedFastaSequenceFile=null;
					CloserUtil.close(sfr);
					CloserUtil.close(sfw);
					}
				}
			}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new SamFixCigar().instanceMainWithExit(args);

	}

}
