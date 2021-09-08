require("data.table")

args = commandArgs(trailingOnly=TRUE)
if (length(args)==0) {
  folder<-"D:/EclipseWorkspaces/workspacePrivate/NINASpeechCommunicationAnalyser/segments_v2/";
}else{
  folder<-args[1]
}

cat("Folder to analyse",folder,"\n")
cat(">\n")
cat("---------------------------------\n")

allDirs<-list.dirs(folder)
ncluster=0
for (i in 1:length(allDirs)){
  if (grepl("/cluster",(allDirs[i]))==T){
    ncluster=ncluster+1
  }
}

clusters<-seq(0,ncluster-1)

for(cl in clusters){
clusterN<-cl

nrgfile = paste(folder,"cluster",clusterN,"/energy.csv",sep="")
pitchfile = paste(folder,"cluster",clusterN,"/pitch.csv",sep="")

#nrgtable<-read.csv(nrgfile,header = F,sep = ",", skip = 1)
nrgtable<-fread(nrgfile,header = F,sep = ",", skip = 1, fill = T)
pitchtable<-fread(pitchfile,header = F,sep = ",", skip = 1, fill = T)


# mean energy in the cluster
nrgvalues<-nrgtable[,2:dim(nrgtable)[2]]
energyVector<-as.vector(as.matrix(nrgvalues))
energyVector<-energyVector[which(!is.na(energyVector))]
  mean_energy<-mean(energyVector)/10^7
#- max min energy in the cluster
  max_energy<-max(energyVector)/10^7
  min_energy<-min(energyVector)/10^7
# mean pitch in the cluster -> mean agitation
pitchVector<-as.vector(as.matrix(pitchtable[,3:dim(pitchtable)[2]]))
pitchVector<-pitchVector[which(!is.na(pitchVector))]
  mean_pitch<-mean(pitchVector)
  max_pitch<-max(pitchVector)
  min_pitch<-min(pitchVector)
# number of questions
questions<-pitchtable[,2]
  number_of_questions<-length(which(questions[[1]]==TRUE))
  perc_of_questions<-length(which(questions[[1]]==TRUE))*100/length(questions[[1]])
# mean signal length in the cluster -> mean overlap
lengths<-c()
nrows<-dim(nrgvalues)[1]
for(i in 1:nrows){
  v<-as.vector(nrgvalues[i,])
  duration<-length(which(!is.na(v)))
  durationSec<-duration*0.1 #in seconds
  lengths<-c(lengths,durationSec)
}
mean_duration_or_overlap<-mean(lengths)

sd_duration_or_overlap<-sd(lengths)
low_duration<-min(lengths) #mean_duration_or_overlap-1.96*sd_duration_or_overlap
high_duration<-max(lengths) #mean_duration_or_overlap+1.96*sd_duration_or_overlap
options(digits=2)

cat("Statistics for cluster",clusterN,"\n")
cat("Mean energy (clarity)",mean_energy,"[",min_energy,",",max_energy,"]","\n")
cat("Mean pitch (tone/agitation)",mean_pitch,"db [",max_pitch,",",min_pitch,"]","\n")
cat("N. of questions",number_of_questions,"[",perc_of_questions,"%]","\n")
cat("Mean duration and overlap",mean_duration_or_overlap,"s [",low_duration,",",high_duration,"]","\n\n")

}
cat("---------------------------------\n")
cat("<\n")

