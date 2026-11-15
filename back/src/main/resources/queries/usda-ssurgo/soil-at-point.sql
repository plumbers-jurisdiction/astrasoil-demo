-- The soil mapped under a point, from USDA SSURGO through Soil Data Access (SQL Server).
-- One row per component and surface-layer texture:
--   map unit (mapunit) -> the soils mixed in it (component), largest share first
--   -> each soil's surface layer (chorizon starting at depth 0) -> that layer's textures (chtexturegrp)
-- Placeholders, filled in by UsdaSsurgoSoilAdapter: {longitude} and {latitude} (WKT puts longitude first).
SELECT mu.mukey, mu.muname,
    c.cokey, c.compname, c.comppct_r, c.majcompflag, c.drainagecl,
    h.chkey, h.om_r, h.ph1to1h2o_r, h.awc_r, h.kwfact, h.sandtotal_r, h.silttotal_r, h.claytotal_r,
    tg.texdesc, tg.rvindicator, tg.stratextsflag
FROM mapunit mu
JOIN component c ON c.mukey = mu.mukey
LEFT JOIN chorizon h ON h.cokey = c.cokey AND h.hzdept_r = 0
LEFT JOIN chtexturegrp tg ON tg.chkey = h.chkey
WHERE mu.mukey IN (SELECT mukey FROM SDA_Get_Mukey_from_intersection_with_WktWgs84('point({longitude} {latitude})'))
ORDER BY c.comppct_r DESC, c.cokey, tg.rvindicator DESC
