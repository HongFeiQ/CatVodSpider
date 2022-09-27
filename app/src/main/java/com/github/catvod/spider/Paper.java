package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.bean.paper.Data;
import com.github.catvod.bean.paper.Item;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttpUtil;
import com.github.catvod.utils.Misc;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author ColaMint & FongMi
 */
public class Paper extends Spider {

    private List<String> types;
    private List<Data> all;
    private Ali ali;

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Misc.CHROME);
        return headers;
    }

    private List<Data> getAll() {
        return all = all != null ? all : Item.objectFrom(OkHttpUtil.string("https://gitcafe.net/alipaper/all.json", getHeaders())).getData();
    }

    @Override
    public void init(Context context, String extend) {
        types = Arrays.asList("hyds", "rhds", "omds", "qtds", "hydy", "rhdy", "omdy", "qtdy", "hydm", "rhdm", "omdm", "jlp", "zyp", "jypx", "qtsp");
        ali = new Ali(extend);
    }

    @Override
    public String homeContent(boolean filter) throws JSONException {
        Document doc = Jsoup.parse(OkHttpUtil.string("https://u.gitcafe.net/", getHeaders()));
        Elements trs = doc.select("table.tableizer-table > tbody > tr");
        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
        List<Class> classes = new ArrayList<>();
        for (Element tr : trs) {
            if (tr.text().contains("音乐")) break;
            List<Filter.Value> values = new ArrayList<>();
            for (Element td : tr.select("td")) {
                if (td.hasClass("tableizer-title")) {
                    String typeId = td.select("a").attr("href").replace("#", "");
                    classes.add(new Class(typeId, td.text()));
                    filters.put(typeId, Arrays.asList(new Filter("type", "類型", values)));
                } else {
                    String value = td.select("a").attr("onclick").split("'")[1];
                    values.add(new Filter.Value(td.text(), value));
                }
            }
        }
        List<Vod> list = new ArrayList<>();
        JSONObject homeData = new JSONObject(OkHttpUtil.string("https://gitcafe.net/alipaper/home.json", getHeaders()));
        List<Data> items = Data.arrayFrom(homeData.getJSONObject("info").getJSONArray("new").toString());
        for (Data item : items) if (types.contains(item.getCat())) list.add(item.getVod());
        return Result.string(classes, list, filters);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        List<Vod> list = new ArrayList<>();
        String type = extend.containsKey("type") ? extend.get("type") : tid;
        List<Data> items = Data.arrayFrom(OkHttpUtil.string("https://gitcafe.net/alipaper/data/" + type + ".json", getHeaders()));
        for (Data item : items) list.add(item.getVod());
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        return ali.detailContent(ids);
    }

    @Override
    public String searchContent(String key, boolean quick) {
        List<Vod> list = new ArrayList<>();
        for (Data item : getAll()) if (types.contains(item.getCat()) && item.getTitle().contains(key)) list.add(item.getVod());
        return Result.string(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return ali.playerContent(flag, id);
    }
}
