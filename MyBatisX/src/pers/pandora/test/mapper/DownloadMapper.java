package pers.pandora.test.mapper;
import pers.pandora.test.Download;

public interface DownloadMapper{
	 Download queryForOne(int id);
	 void update(Download entity);
	 void insert(Download entity);
	 void deleteById(int id);
}